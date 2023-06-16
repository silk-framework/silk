package controllers.workspaceApi.search.activity

import controllers.workspaceApi.search.SearchApiModel.{FacetSetting, FacetedSearchRequest, FacetedSearchResult, SearchRequestTrait, SortOrder, SortableProperty, TypedTasks}
import controllers.workspaceApi.search._
import controllers.workspaceApi.search.activity.ActivitySearchRequest.{ActivityResult, ActivitySortBy}
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workbench.workspace.{WorkbenchAccessMonitor, WorkspaceItem, WorkspaceProject, WorkspaceTask}
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{Json, OFormat, Reads}

case class ActivitySearchRequest(@Schema(
                                   description = "If defined, only activities from that project are fetched.",
                                   nullable = true
                                 )
                                 project: Option[String] = None,
                                 @Schema(
                                   description = "If defined, only activities of this parent type are fetched.",
                                   nullable = true,
                                   implementation = classOf[String], // The ItemType JSON reader expects a single string instead of the whole object
                                   allowableValues = Array("workspace", "project", "dataset", "transform", "linking", "workflow", "task")
                                 )
                                 itemType: Option[ItemType] = None,
                                 @Schema(
                                   description = "Conjunctive multi word query. The single words can be scattered over different artifact properties.",
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
                                   description = "Search result limit to allow for paging. Can be disabled by setting it to '0', which will return all results.",
                                   nullable = true,
                                   defaultValue = "10",
                                   implementation = classOf[Int]
                                 )
                                 limit: Option[Int] = None,
                                 @Schema(
                                   description = "Optional sort parameter allows for sorting the result list by a specific artifact property, e.g. label, update date or running time.",
                                   nullable = true,
                                   defaultValue = "label",
                                   allowableValues = Array("label", "recentlyUpdated", "runningTime")
                                 )
                                 sortBy: Option[ActivitySortBy.Value] = None,
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
                                 facets: Option[Seq[FacetSetting]] = None) extends SearchRequestTrait  {

  /** The offset used for paging. */
  def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

  /** The limit used for paging. */
  def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

  def workingSortBy: ActivitySortBy.Value = sortBy.getOrElse(ActivitySortBy.label)

  /** Execute search request and return result list. */
  def apply()(implicit userContext: UserContext,
              accessMonitor: WorkbenchAccessMonitor): FacetedSearchResult = {
    // Retrieve selected projects and tasks
    val selectedProjects: Seq[Project] = projects
    val tasks: Seq[TypedTasks] = selectedProjects.flatMap(TypedTasks.fetchTasks(_, itemType))

    // Retrieve all activities for the selected projects and tasks
    var activities = retrieveActivities(selectedProjects, tasks, includeGlobal = project.isEmpty)

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
      sortByProperties = Seq(SortableProperty("label", "Parent label"), SortableProperty("recentlyUpdated", "Recently updated"), SortableProperty("runningTime", "Running time")),
      facets = facetResults
    )
  }

  private def retrieveActivities(projects: Seq[Project], tasks: Seq[TypedTasks], includeGlobal: Boolean)
                                (implicit userContext: UserContext) : Seq[WorkspaceActivity[_]] = {
    def globalActivities: Seq[WorkspaceActivity[_]] = if(includeGlobal) WorkspaceFactory().workspace.activities else Seq.empty
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

  private def filterActivities(activities: Seq[WorkspaceActivity[_]], lowerCaseTerms: Seq[String])
                              (implicit userContext: UserContext): Seq[WorkspaceActivity[_]] = {
    activities.filter(filterActivity(_, lowerCaseTerms))
  }

  private def filterActivity(activity: WorkspaceActivity[_], lowerCaseTerms: Seq[String])
                            (implicit userContext: UserContext): Boolean = {
    val taskLabel = activity.taskOption.map(_.fullLabel).getOrElse("")
    // We do search in the project if no project is preselected
    // Additionally, we do always search in the project label of project activities.
    val searchInProject = project.isEmpty || (activity.taskOption.isEmpty && activity.projectOpt.isDefined)
    val projectLabel = if(searchInProject) activity.projectOpt.map(_.fullLabel).getOrElse("") else ""
    matchesSearchTerm(lowerCaseTerms, activity.label, taskLabel, projectLabel)
  }

  // Sort results according to request
  private def sort(activities: Seq[WorkspaceActivity[_]])
                  (implicit accessMonitor: WorkbenchAccessMonitor,
                   userContext: UserContext): Seq[WorkspaceActivity[_]] = {
    val sortedActivities = sortBy match {
      case Some(ActivitySortBy.label) =>
        activities.sortBy { activity =>
          activity.taskOption.orElse(activity.projectOpt).map(_.fullLabel).getOrElse("")
        }
      case Some(ActivitySortBy.recentlyUpdated) =>
        activities.sortBy { activity =>
          -activity.status().timestamp
        }
      case Some(ActivitySortBy.runningTime) =>
        activities.sortBy { activity =>
          activity.startTime match {
            case Some(startTime) =>
              val status = activity.status()
              if(status.isRunning) {
                startTime.toEpochMilli - System.currentTimeMillis()
              } else {
                startTime.toEpochMilli - status.timestamp
              }
            case None =>
              1L // Activity has never been executed -> Sort to the end
          }
        }
      case None => // Sort by recently viewed parent
        val userAccessItems = accessMonitor.getAccessItems.reverse // last item is the most recent item, so reverse
        val userAccessIndexMap = userAccessItems.zipWithIndex.toMap
        activities.sortBy { activity =>
          toWorkspaceItem(activity).flatMap(userAccessIndexMap.get).getOrElse(Int.MaxValue)
        }
    }

    if(sortOrder.contains(SortOrder.DESC)) {
      sortedActivities.reverse
    } else {
      sortedActivities
    }
  }

  private def toWorkspaceItem(activity: WorkspaceActivity[_]): Option[WorkspaceItem] = {
    activity.taskOption match {
      case Some(task) =>
        Some(WorkspaceTask(task.project.id, task.id))
      case None =>
        activity.projectOpt match {
          case Some(project) =>
            Some(WorkspaceProject(project.id))
          case None =>
            None
        }
    }
  }

}

object ActivitySearchRequest {

  object ActivitySortBy extends Enumeration {
    val label, recentlyUpdated, runningTime = Value
  }

  @Schema(description = "Search result describing an activity.")
  case class ActivityResult(id: String,
                            label: String,
                            project: Option[String],
                            projectLabel: Option[String],
                            task: Option[String],
                            taskLabel: Option[String],
                            parentType: String,
                            isCacheActivity: Boolean)

  object ActivityResult {
    def apply(activity: WorkspaceActivity[_]): ActivityResult = {
      ActivityResult(
        id = activity.name,
        label = activity.label,
        project = activity.projectOpt.map(_.id.toString),
        projectLabel = activity.projectOpt.map(_.fullLabel),
        task = activity.taskOption.map(_.id.toString),
        taskLabel = activity.taskOption.map(_.fullLabel),
        parentType = itemType(activity).id,
        isCacheActivity = activity.isCacheActivity
      )
    }

    @inline
    private def itemType(activity: WorkspaceActivity[_]): ItemType = {
      activity.taskOption match {
        case Some(task) =>
          ItemType.itemType(task.data)
        case None if activity.projectOpt.isDefined  =>
          ItemType.project
        case _ =>
          ItemType.global
      }
    }
  }

  implicit val activityResultFormat: OFormat[ActivityResult] = Json.format[ActivityResult]
  implicit val activitySortByFormat: Reads[ActivitySortBy.Value] = Reads.enumNameReads(ActivitySortBy)
  implicit val activitySearchRequestReader: Reads[ActivitySearchRequest] = Json.reads[ActivitySearchRequest]

}

