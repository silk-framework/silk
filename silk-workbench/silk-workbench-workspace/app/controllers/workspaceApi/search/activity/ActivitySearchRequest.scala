package controllers.workspaceApi.search.activity

import controllers.workspaceApi.search.SearchApiModel.{FacetSetting, FacetedSearchRequest, FacetedSearchResult, SearchRequestTrait, SortBy, SortOrder, SortableProperty, TypedTasks}
import controllers.workspaceApi.search._
import controllers.workspaceApi.search.activity.ActivitySearchRequest.ActivityResult
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{Json, OFormat, Reads}

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
    // Retrieve selected projects and tasks
    val ps: Seq[Project] = projects
    val tasks: Seq[TypedTasks] = ps.flatMap(TypedTasks.fetchTasks(_, itemType))
    val selectedProjects: Seq[Project] = if(project.isEmpty && (itemType.contains(ItemType.project) || itemType.isEmpty)) ps else Seq()

    // Retrieve all activities for the selected projects and tasks
    var activities = retrieveActivities(selectedProjects, tasks)

    // Filter activities by search terms
    for(term <- textQuery if term.trim.nonEmpty) {
      val lowerCaseTerm = extractSearchTerms(term)
      activities = filterActivities(activities, lowerCaseTerm)
    }

    // Filter activities by facets
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

    val resultWindowJson = resultWindow.map(a => Json.toJsObject(ActivityResult(a)))
    val facetResults = facetCollectors.result

    FacetedSearchResult(
      total = sorted.size,
      results = resultWindowJson,
      sortByProperties = Seq(SortableProperty("label", "Label")),
      facets = facetResults
    )
  }

  private def retrieveActivities(projects: Seq[Project], tasks: Seq[TypedTasks])
                                (implicit userContext: UserContext) : Seq[WorkspaceActivity[_]] = {
    def globalActivities: Seq[WorkspaceActivity[_]] = WorkspaceFactory().workspace.activities
    def projectActivities: Seq[WorkspaceActivity[_]] = projects.flatMap(_.activities)
    def taskActivities: Seq[WorkspaceActivity[_]] = tasks.flatMap(_.tasks).flatMap(_.activities.asInstanceOf[Seq[WorkspaceActivity[_]]])

    itemType match {
      case None =>
        globalActivities ++ projectActivities ++ taskActivities
      case Some(ItemType.global) =>
        globalActivities
      case Some(ItemType.project) =>
        projectActivities
      case _ =>
        taskActivities
    }
  }

  private def filterActivities(activities: Seq[WorkspaceActivity[_]], lowerCaseTerms: Seq[String]): Seq[WorkspaceActivity[_]] = {
    activities.filter(filterActivity(_, lowerCaseTerms))
  }

  private def filterActivity(activity: WorkspaceActivity[_], lowerCaseTerms: Seq[String]): Boolean = {
    val taskLabel = activity.taskOption.map(_.fullLabel).getOrElse("")
    matchesSearchTerm(lowerCaseTerms, activity.label, taskLabel)
  }

  // Sort results according to request
  private def sort(activities: Seq[WorkspaceActivity[_]])
                  (implicit accessMonitor: WorkbenchAccessMonitor,
                   userContext: UserContext): Seq[WorkspaceActivity[_]] = {
    // TODO
    activities
  }

}

object ActivitySearchRequest {

  @Schema(description = "Search result describing an activity.")
  case class ActivityResult(id: String,
                            label: String,
                            project: Option[String],
                            task: Option[String])

  object ActivityResult {
    def apply(activity: WorkspaceActivity[_]): ActivityResult = {
      ActivityResult(
        id = activity.name,
        label = activity.label,
        project = activity.projectOpt.map(_.id.toString),
        task = activity.taskOption.map(_.id.toString)
      )
    }
  }

  implicit val activityResultFormat: OFormat[ActivityResult] = Json.format[ActivityResult]
  implicit val activitySearchRequestReader: Reads[ActivitySearchRequest] = Json.reads[ActivitySearchRequest]

}

