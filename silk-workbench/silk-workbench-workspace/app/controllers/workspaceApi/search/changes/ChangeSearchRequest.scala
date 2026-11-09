package controllers.workspaceApi.search.changes

import controllers.projectApi.ChangeJournalApi.ChangeEntryJson
import controllers.workspaceApi.search.SearchApiModel._
import controllers.workspaceApi.search.changes.ChangeSearchRequest.{ChangeItem, ChangeSearchResult, ChangeSortBy}
import controllers.workspaceApi.search.{FacetCollector, FacetResult, IdAndLabelKeywordFacetCollector, ItemTypeFacetCollector, ItemTypeFacetCollectors}
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.Project
import org.silkframework.workspace.changes.ChangeHeader
import play.api.libs.json.{Format, Json, Reads}

/**
  * A search over the change journal of a project, like the activity search: a text query, facets, a sort property and a
  * page window over the headers the journal holds. Answered from one snapshot of the journal, so that the entries'
  * flags agree with the numbers that come with them.
  */
case class ChangeSearchRequest(@Schema(
                                 description = "Conjunctive multi word query, matched against the description of a change, the task it " +
                                   "concerns, the user and the origin.",
                                 nullable = true
                               )
                               textQuery: Option[String] = None,
                               @Schema(
                                 description = "Search result offset to allow for paging.",
                                 nullable = true,
                                 implementation = classOf[Int]
                               )
                               offset: Option[Int] = None,
                               @Schema(
                                 description = "Search result limit to allow for paging. Can be disabled by setting it to '0', which returns all results.",
                                 nullable = true,
                                 defaultValue = "10",
                                 implementation = classOf[Int]
                               )
                               limit: Option[Int] = None,
                               @Schema(
                                 description = "The property to sort by. Without one the changes come newest first; 'time' ascending is oldest first.",
                                 nullable = true,
                                 allowableValues = Array("time")
                               )
                               sortBy: Option[ChangeSortBy.Value] = None,
                               @Schema(
                                 description = "The order of the retrieved results.",
                                 nullable = true,
                                 defaultValue = "ASC",
                                 allowableValues = Array("ASC", "DESC")
                               )
                               sortOrder: Option[SortOrder.Value] = None,
                               @ArraySchema(
                                 schema = new Schema(
                                   description = "Defines what facets are set to which values. The 'keyword' facet allows multiple values to be set.",
                                   nullable = true,
                                   implementation = classOf[String]
                               ))
                               facets: Option[Seq[FacetSetting]] = None) extends SearchRequestTrait {

  // The project comes with the URL, not with the body
  override def project: Option[String] = None

  def workingOffset: Int = offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

  def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

  /** Runs the search over the journal of the project. */
  def apply(project: Project)(implicit userContext: UserContext): ChangeSearchResult = {
    val facetSettings = facets.getOrElse(Seq.empty)
    // A facet of another search would match everything rather than fail
    val unknownFacets = facetSettings.map(_.facetId).filterNot(ChangeSearchRequest.facetIds.contains)
    if(unknownFacets.nonEmpty) {
      throw BadUserInputException(s"Unknown facets for the change search: ${unknownFacets.mkString(", ")}. " +
        s"Available: ${ChangeSearchRequest.facetIds.mkString(", ")}.")
    }
    val journal = project.changeJournal
    val (headers, reviewedUpTo, latestSeq) = journal.snapshotWithLatestSeq
    val revertedBy = journal.revertedBy(headers)
    val fulfilledBy = journal.fulfilledBy(headers)
    val unreviewed = journal.unreviewed(headers, reviewedUpTo).map(_.seq).toSet
    // A task is named as it is now; a task that is gone keeps its id
    val taskLabels = project.allTasks.map(task => task.id -> task.labelOrId).toMap
    var items = headers.map { header =>
      ChangeItem(header, revertedBy.get(header.seq), fulfilledBy.get(header.seq), unreviewed.contains(header.seq),
        header.taskId.map(id => taskLabels.getOrElse(id, id.toString)))
    }
    for(term <- textQuery if term.trim.nonEmpty) {
      val terms = extractSearchTerms(term)
      items = items.filter(item => matchesSearchTerm(terms, item.searchIn: _*))
    }
    val collectors = ItemTypeFacetCollectors(Seq(ChangeFacetCollector()))
    items = items.filter(item => collectors.filterAndCollect(item, facetSettings))
    val sorted = sort(items)
    val window = if(workingLimit != 0) sorted.slice(workingOffset, workingOffset + workingLimit) else sorted.drop(workingOffset)
    ChangeSearchResult(
      total = sorted.size,
      results = window.map(item => ChangeEntryJson.of(project, item.header, item.revertedBy, item.fulfilledBy, item.unreviewed)),
      sortByProperties = ChangeSearchRequest.sortableProperties,
      facets = collectors.result,
      reviewedUpTo = reviewedUpTo,
      latestSeq = latestSeq,
      unreviewed = unreviewed.size
    )
  }

  // Newest first without a sort property; by time, ascending is oldest first. A journal is read in its order, so the
  // other properties are facets rather than sort properties.
  private def sort(items: Seq[ChangeItem]): Seq[ChangeItem] = {
    val newestFirst = items.sortBy(-_.header.seq)
    sortBy match {
      case Some(ChangeSortBy.time) if !sortOrder.contains(SortOrder.DESC) => newestFirst.reverse
      case _ => newestFirst
    }
  }
}

object ChangeSearchRequest {

  object ChangeSortBy extends Enumeration {
    val time = Value
  }

  val sortableProperties: Seq[SortableProperty] = Seq(SortableProperty(ChangeSortBy.time.toString, "Time"))

  /** The ids of the facets the search offers; the request validation accepts every registered facet. */
  val facetIds: Seq[String] = ChangeFacetCollector().facetCollectors.map(_.appliesForFacet.id)

  /** Names a user for display as the changes page does: the last segment of the URI. */
  def userLabel(uri: String): String = {
    val idx = Seq(uri.lastIndexOf('/'), uri.lastIndexOf(':'), uri.lastIndexOf('#')).max
    if(idx >= 0 && idx < uri.length - 1) uri.substring(idx + 1) else uri
  }

  /** A change as the search sees it: the header with what the journal derives for it, and the task's current name. */
  case class ChangeItem(header: ChangeHeader, revertedBy: Option[Int], fulfilledBy: Option[Int], unreviewed: Boolean,
                        taskLabel: Option[String]) {

    def kind: String = header.kind

    def userLabel: String = header.user.map(ChangeSearchRequest.userLabel).getOrElse("")

    /** What the text query is matched against. */
    def searchIn: Seq[String] = Seq(header.describe, taskLabel.getOrElse(""), userLabel, header.origin.getOrElse(""))

    /** The states the change is in, by the ids of the state facet; a change can be in several. */
    def states: Set[String] = Set(
      Option.when(unreviewed)("unreviewed"),
      Option.when(revertedBy.isDefined)("reverted"),
      Option.when(header.reverts.isDefined)("revert"),
      Option.when(header.proposal && revertedBy.isEmpty && fulfilledBy.isEmpty)("proposal"),
      Option.when(fulfilledBy.isDefined)("fulfilled")
    ).flatten
  }

  /** The search result: the page of entries as the listing serves them, the facets, and the journal's numbers. */
  case class ChangeSearchResult(@Schema(description = "How many changes match, beyond the page.")
                                total: Int,
                                @ArraySchema(schema = new Schema(implementation = classOf[ChangeEntryJson]))
                                results: Seq[ChangeEntryJson],
                                @ArraySchema(schema = new Schema(implementation = classOf[SortableProperty]))
                                sortByProperties: Seq[SortableProperty],
                                @ArraySchema(schema = new Schema(implementation = classOf[FacetResult]))
                                facets: Seq[FacetResult],
                                @Schema(description = "The seq up to which no change waits for review.")
                                reviewedUpTo: Int,
                                @Schema(description = "The latest seq of the journal as the store counts it, read with the snapshot the page was taken from; marking reviewed sends it. 0 if there is none.")
                                latestSeq: Int,
                                @Schema(description = "How many changes are unreviewed in the whole journal, whatever the query matches.")
                                unreviewed: Int)

  implicit val changeSearchResultFormat: Format[ChangeSearchResult] = Json.format[ChangeSearchResult]
  implicit val changeSortByReads: Reads[ChangeSortBy.Value] = Reads.enumNameReads(ChangeSortBy)
  implicit val changeSearchRequestReads: Reads[ChangeSearchRequest] = Json.reads[ChangeSearchRequest]
}

/** The facets of the change search. */
case class ChangeFacetCollector() extends ItemTypeFacetCollector[ChangeItem] {
  override val facetCollectors: Seq[FacetCollector[ChangeItem]] = Seq(
    ChangeKindCollector(),
    ChangeStateCollector(),
    ChangeOriginCollector(),
    ChangeUserCollector(),
    ChangeTaskCollector()
  )
}

case class ChangeKindCollector() extends IdAndLabelKeywordFacetCollector[ChangeItem] {
  override def appliesForFacet: Facet = Facets.changeKind

  override protected def extractIdAndLabel(item: ChangeItem)(implicit user: UserContext): Set[(String, String)] = {
    Set((item.kind, item.kind.capitalize))
  }
}

case class ChangeStateCollector() extends IdAndLabelKeywordFacetCollector[ChangeItem] {
  private val labels = Map(
    "unreviewed" -> "Unreviewed",
    "reverted" -> "Reverted",
    "revert" -> "Reverts a change",
    "proposal" -> "Proposed run",
    "fulfilled" -> "Run as proposed"
  )

  override def appliesForFacet: Facet = Facets.changeState

  override protected def extractIdAndLabel(item: ChangeItem)(implicit user: UserContext): Set[(String, String)] = {
    item.states.map(state => (state, labels(state)))
  }
}

/** A person, or the client an agent change came from, without the client's version, so that one client is one value. */
case class ChangeOriginCollector() extends IdAndLabelKeywordFacetCollector[ChangeItem] {
  override def appliesForFacet: Facet = Facets.changeOrigin

  override protected def extractIdAndLabel(item: ChangeItem)(implicit user: UserContext): Set[(String, String)] = {
    item.header.origin match {
      case Some(origin) =>
        val client = ChangeOriginCollector.client(origin)
        Set((client, client))
      case None => Set((ChangeOriginCollector.person, "Person"))
    }
  }
}

object ChangeOriginCollector {
  final val person = "person"

  /** The client of an origin without its version, e.g. 'mcp:claude-code' of 'mcp:claude-code/2.1.309 (cli)'. */
  def client(origin: String): String = origin.takeWhile(_ != '/').trim
}

case class ChangeUserCollector() extends IdAndLabelKeywordFacetCollector[ChangeItem] {
  override def appliesForFacet: Facet = Facets.changeUser

  override protected def extractIdAndLabel(item: ChangeItem)(implicit user: UserContext): Set[(String, String)] = {
    item.header.user.map(uri => (uri, ChangeSearchRequest.userLabel(uri))).toSet
  }
}

case class ChangeTaskCollector() extends IdAndLabelKeywordFacetCollector[ChangeItem] {
  override def appliesForFacet: Facet = Facets.changeTask

  override protected def extractIdAndLabel(item: ChangeItem)(implicit user: UserContext): Set[(String, String)] = {
    (for(id <- item.header.taskId; label <- item.taskLabel) yield (id.toString, label)).toSet
  }
}
