package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, FacetSetting, FacetType, FacetedSearchRequest, FacetedSearchResult, ID, LABEL, SearchRequestTrait, SortBy, SortOrder, SortableProperty, TypedTasks}
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{JsNull, JsObject, JsString, Json, Reads}

case class ActivitySearchRequest(@Schema(
                                   description = "If defined, only artifacts from that project are fetched.",
                                   required = false,
                                   nullable = true
                                 )
                                 project: Option[String] = None,
                                 @Schema(
                                   description = "If defined, only artifacts of this type are fetched.",
                                   required = false,
                                   nullable = true,
                                   implementation = classOf[String], // The ItemType JSON reader expects a single string instead of the whole object
                                   allowableValues = Array("workspace", "project", "dataset", "transform", "linking", "workflow", "task")
                                 )
                                 itemType: Option[ItemType] = None,
                                 @Schema(
                                   description = "Conjunctive multi word query. The single words can be scattered over different artifact properties, e.g. one in label and one in description.",
                                   required = false,
                                   nullable = true
                                 )
                                 textQuery: Option[String] = None,
                                 @Schema(
                                   description = "Search result offset to allow for paging.",
                                   required = false,
                                   nullable = true,
                                   implementation = classOf[Int]
                                 )
                                 offset: Option[Int] = None,
                                 @Schema(
                                   description = "Search result limit to allow for paging. Can be disabled by setting it to '0', which will return all results.",
                                   required = false,
                                   nullable = true,
                                   defaultValue = "10",
                                   implementation = classOf[Int]
                                 )
                                 limit: Option[Int] = None,
                                 @Schema(
                                   description = "Optional sort parameter allows for sorting the result list by a specific artifact property, e.g. label, creation date, update date.",
                                   required = false,
                                   nullable = true,
                                   allowableValues = Array("label")
                                 )
                                 sortBy: Option[SortBy.Value] = None,
                                 @Schema(
                                   description = "If defined, only artifacts from that project are fetched.",
                                   required = false,
                                   nullable = true,
                                   allowableValues = Array("ASC", "DESC")
                                 )
                                 sortOrder: Option[SortOrder.Value] = None,
                                 @ArraySchema(
                                   schema = new Schema(
                                     description = "Defines what facets are set to which values. The 'keyword' facet allows multiple values to be set.",
                                     required = false,
                                     nullable = true,
                                     implementation = classOf[String]
                                 ))
                                 facets: Option[Seq[FacetSetting]] = None) extends SearchRequestTrait  {

  /** The offset used for paging. */
  def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

  /** The limit used for paging. */
  def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

  /** Execute search request and return result list. */
  def apply()(implicit userContext: UserContext,
              accessMonitor: WorkbenchAccessMonitor): FacetedSearchResult = {
    val ps: Seq[Project] = projects
    var tasks: Seq[TypedTasks] = ps.flatMap(TypedTasks.fetchTasks(_, itemType))
    var selectedProjects: Seq[Project] = if(project.isEmpty && (itemType.contains(ItemType.project) || itemType.isEmpty)) ps else Seq()

    for(term <- textQuery if term.trim.nonEmpty) {
      val lowerCaseTerm = extractSearchTerms(term)
      tasks = tasks.map(typedTasks => filterTasksByTextQuery(typedTasks, lowerCaseTerm))
      selectedProjects = if(itemType.contains(ItemType.project) || itemType.isEmpty) selectedProjects.filter(p => matchesSearchTerm(lowerCaseTerm, p)) else Seq()
    }

    def globalActivities = WorkspaceFactory().workspace.activities
    def projectActivities = selectedProjects.flatMap(_.activities)
    def taskActivities = tasks.flatMap(_.tasks).flatMap(_.activities.asInstanceOf[Seq[WorkspaceActivity[_]]])

    var activities = itemType match {
      case None =>
        globalActivities ++ projectActivities ++ taskActivities
      case Some(ItemType.global) =>
        globalActivities
      case Some(ItemType.project) =>
        projectActivities
      case _ =>
        taskActivities
    }

    // facets are collected after filtering, so only non empty facets are displayed with correct counts
    val facetCollectors = ItemTypeFacetCollectors(Seq(ActivityFacetCollector()))
    val facetSettings = facets.getOrElse(Seq.empty)

    activities = activities.filter(activity => facetCollectors.filterAndCollect(activity, facetSettings))

    val sorted = sort(activities)
    val resultWindow =
      if(workingLimit != 0) {
        sorted.slice(workingOffset, workingOffset + workingLimit)
      } else {
        sorted.drop(workingOffset)
      }

    val resultWindowJson = resultWindow.map(toJson)
    val facetResults = facetCollectors.result

    FacetedSearchResult(
      total = sorted.size,
      results = resultWindowJson,
      sortByProperties = Seq(SortableProperty("label", "Label")),
      facets = facetResults
    )
  }

  private def filterTasksByTextQuery(typedTasks: TypedTasks,
                                     lowerCaseTerms: Seq[String])
                                    (implicit userContext: UserContext): TypedTasks = {
    typedTasks.copy(tasks = typedTasks.tasks.filter { task =>
      // Project is shown in search results when not restricting by project. Task properties are not shown.
      matchesSearchTerm(lowerCaseTerms, task, matchTaskProperties = false, matchProject = project.isEmpty) })
  }

  // Sort results according to request
  private def sort(activities: Seq[WorkspaceActivity[_]])
                  (implicit accessMonitor: WorkbenchAccessMonitor,
                   userContext: UserContext): Seq[WorkspaceActivity[_]] = {
    // TODO
    activities
  }


  private def toJson(activity: WorkspaceActivity[_])
                    (implicit userContext: UserContext): JsObject = {
    JsObject(
      Seq(
        ID -> JsString(activity.name),
        LABEL -> JsString(activity.label),
        "project" -> activity.projectOpt.map(_.name.toString).map(JsString(_)).getOrElse(JsNull),
        "task" -> activity.taskOption.map(_.id.toString).map(JsString(_)).getOrElse(JsNull)
      )
    )
  }

}

object ActivitySearchRequest {

  case class ActivityResult()

  object ActivityResult {

  }

  implicit val activitySearchRequestReader: Reads[ActivitySearchRequest] = Json.reads[ActivitySearchRequest]

}

object ActivityFacets {
  final val status: Facet = Facet("status", "Status", "The activity status.", FacetType.keyword)

  val facetIds: Seq[String] = Seq(status).map(_.id)
  assert(facetIds.distinct.size == facetIds.size, "Facet IDs must be unique!")
}

case class ActivityStatusCollector() extends NoLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = ActivityFacets.status

  override def extractKeywordIds(activity: WorkspaceActivity[_])
                                (implicit user: UserContext): Set[String] = {
    Set(activity.status().name)
  }

}

case class ActivityFacetCollector() extends ItemTypeFacetCollector[WorkspaceActivity[_]] {
  override val facetCollectors: Seq[FacetCollector[WorkspaceActivity[_]]] = Seq(
    ActivityStatusCollector(),
  )
}

case class OverallActivityFacetCollector() {

  private val facetCollectors = ItemTypeFacetCollectors(Seq(ActivityFacetCollector()))

  def filterAndCollect(activity: WorkspaceActivity[_],
                       facetSettings: Seq[FacetSetting])
                      (implicit user: UserContext): Boolean = {
    facetCollectors.filterAndCollect(activity, facetSettings)
  }

  def results: Iterable[FacetResult] = {
    facetCollectors.result
  }
}
