package controllers.workspaceApi.search

import controllers.util.TextSearchUtils
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, PluginDescription}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers.{TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.serialization.json.MetaDataSerializers.FullTag
import org.silkframework.workbench.workspace.{WorkbenchAccessMonitor, WorkspaceItem, WorkspaceProject, WorkspaceTask}
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Data structures used for handling search requests
  */
object SearchApiModel {
  // Property names
  final val LABEL = "label"
  final val ID = "id"
  final val TYPE = "type"
  final val VALUES = "values"
  final val DESCRIPTION = "description"
  final val PROJECT_ID = "projectId"
  final val PROJECT_LABEL = "projectLabel"
  final val PLUGIN_ID = "pluginId"
  final val PLUGIN_LABEL = "pluginLabel"
  final val TAGS = "tags"
  final val PARAMETERS = "parameters"
  final val READ_ONLY = "readOnly"
  final val URI_PROPERTY = "uriProperty"
  // type values
  final val PROJECT_TYPE = "project"
  /* JSON serialization */
  lazy implicit val responseOptionsReader: Reads[TaskFormatOptions] = Json.reads[TaskFormatOptions]
  lazy implicit val searchRequestReader: Reads[SearchRequest] = Json.reads[SearchRequest]
  lazy implicit val sortOrderReads: Reads[SortOrder.Value] = Reads.enumNameReads(SortOrder)
  lazy implicit val sortByReads: Reads[SortBy.Value] = Reads.enumNameReads(SortBy)
  lazy implicit val facetTypesReads: Reads[FacetType.Value] = Reads.enumNameReads(FacetType)
  lazy implicit val facetSettingReads: Reads[FacetSetting] = new Reads[FacetSetting] {
    override def reads(json: JsValue): JsResult[FacetSetting] = {
      (json \ TYPE).toOption.map(_.as[String]) match {
        case Some(facetType) if FacetType.keyword.toString == facetType => Json.fromJson[KeywordFacetSetting](json)
        case Some(invalidType) => throw BadUserInputException("No valid facet type specified: '" + invalidType + "'. Valid values are: " +
            FacetType.facetTypeSet.mkString(", "))
        case None => throw BadUserInputException("No 'type' property found in given JSON: " + json.toString())
      }
    }
  }
  lazy implicit val keywordFacetValueReads: Format[KeywordFacetValue] = Json.format[KeywordFacetValue]
  lazy implicit val facetResultWrites: Format[FacetResult] = new Format[FacetResult] {
    override def writes(facetResult: FacetResult): JsValue = {
      assert(FacetType.facetTypeSet.contains(facetResult.`type`), s"Facet type '${facetResult.`type`}' is not a valid facet type.")
      val facetValues: Seq[JsValue] = FacetType.withName(facetResult.`type`) match {
        case FacetType.keyword => facetResult.values map {
          case value: KeywordFacetValue => keywordFacetValueReads.writes(value)
        }
      }
      JsObject(Seq(
        ID -> JsString(facetResult.id),
        LABEL -> JsString(facetResult.label),
        DESCRIPTION -> JsString(facetResult.description),
        TYPE -> JsString(facetResult.`type`),
        VALUES -> JsArray(facetValues)
      ))
    }
    override def reads(json: JsValue): JsResult[FacetResult] = {
      val facetType = (json \ SearchApiModel.TYPE).get.as[String]
      assert(FacetType.facetTypeSet.contains(facetType), "Facet type invalid!")
      val facetValues = FacetType.withName(facetType) match {
        case FacetType.keyword =>
          (json \ SearchApiModel.VALUES).as[JsArray].value.map(keywordFacetValueReads.reads(_).get)
      }
      JsSuccess(FacetResult(
        id = (json \ SearchApiModel.ID).as[String],
        label = (json \ SearchApiModel.LABEL).as[String],
        description = (json \ SearchApiModel.DESCRIPTION).as[String],
        `type` = facetType,
        values = facetValues.toIndexedSeq
      ))
    }
  }

  /** The properties that can be sorted by. */
  object SortBy extends Enumeration {
    val label = Value
  }

  /** Sort order, ascending or descending. */
  object SortOrder extends Enumeration {
    val ASC, DESC = Value
  }

  /** The facet types that will correspond to a specific facet widget, e.g. keyword, number/date range. */
  object FacetType extends Enumeration {
    val keyword = Value

    val facetTypeSet: Set[String] = Set(keyword.toString)
  }

  /** Single facet filter setting */
  sealed trait FacetSetting {
    def `type`: FacetType.Value
    def facetId: String
  }
  case class KeywordFacetSetting(`type`: FacetType.Value,
                                 facetId: String,
                                 keywordIds: Set[String]) extends FacetSetting {
    if(!Facets.facetIds.contains(facetId)) {
      throw BadUserInputException(s"Unknown facet ID '$facetId'! Supported facet ID: " + Facets.facetIds.mkString(", "))
    }
  }

  object KeywordFacetSetting {
    implicit val keywordFacetSettingReads: Reads[KeywordFacetSetting] = Json.reads[KeywordFacetSetting]
  }

  object FacetedSearchRequest {
    final val DEFAULT_OFFSET = 0
    final val DEFAULT_LIMIT = 10
  }

  /** Common methods shared between all search requests */
  trait SearchRequestTrait {
    def project: Option[String]

    /**
      * Retrieves all projects that are selected by the request.
      */
    protected def projects(implicit userContext: UserContext): Seq[Project] = {
      project match {
        case Some(projectName) =>
          Seq(WorkspaceFactory().workspace.project(projectName))
        case None =>
          WorkspaceFactory().workspace.projects
      }
    }

    /** Checks if a task matches the search term.
      *
      * @param lowerCaseSearchTerms The search terms.
      * @param task                 The project task that we search in.
      * @param matchTaskProperties  If the property values of the task should also be searched, e.g. when the properties
      *                             are displayed in the search results.
      * @param matchProject         If the project should also be searched in, e.g. the project is displayed in the
      *                             search results.
      **/
    protected def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String],
                                    task: ProjectTask[_ <: TaskSpec],
                                    matchTaskProperties: Boolean,
                                    matchProject: Boolean)(implicit userContext: UserContext): Boolean = {
      val pluginLabel = PluginDescription.forTask(task).label
      val taskLabel = task.fullLabel
      val description = task.metaData.description.getOrElse("")
      val searchInProperties = if(matchTaskProperties) task.data.properties(PluginContext.fromProject(task.project)).map(p => p._2).mkString(" ") else ""
      val searchInProject = if(matchProject) label(task.project) else ""
      val searchInItemType = if(task.data.isInstanceOf[DatasetSpec[_]]) "dataset" else ""
      val tagLabels = task.tags().map(_.label)
      val searchInTerms = Seq(taskLabel, description, searchInProperties, searchInProject, pluginLabel, searchInItemType) ++ tagLabels
      matchesSearchTerm(lowerCaseSearchTerms, searchInTerms: _*)
    }

    /** Match search terms against project. */
    protected def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String], project: Project): Boolean = {
      val id = project.config.id
      val label = project.config.metaData.label.getOrElse("")
      val description = project.config.metaData.description.getOrElse("")
      matchesSearchTerm(lowerCaseSearchTerms, id, label, description, "project")
    }

    protected def extractSearchTerms(term: String): Array[String] = {
      TextSearchUtils.extractSearchTerms(term)
    }

    protected def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String], searchIn: String*): Boolean = {
      TextSearchUtils.matchesSearchTerm(lowerCaseSearchTerms, searchIn :_*)
    }
  }

  /**
    * Representation of a facet.
    * @param id          The facet ID.
    * @param label       The facet label.
    * @param description The facet description.
    * @param facetType   The facet type, e.g. keyword facet, numeric range facet etc.
    */
  case class Facet(id: String, label: String, description: String, facetType: FacetType.Value)

  object Facets {
    // Dataset facets
    final val datasetType: Facet = Facet("datasetType", "Dataset type", "The concrete type of a dataset, e.g. its data model and format etc.", FacetType.keyword)
    final val fileResource: Facet = Facet("datasetFileResource", "Dataset file", "The file resource of a file based dataset.", FacetType.keyword)
    // Transformation facets
    final val transformInputResource: Facet = Facet("transformInputResource", "Transformed File Resource",
      "In case the input is a file resource based dataset, these are the file names of these resources.", FacetType.keyword)
    // Workflow facets
    final val workflowExecutionStatus: Facet = Facet("workflowExecutionStatus", "Last Execution Status", "Allows to filter by the" +
        " status of the last execution of the workflow.", FacetType.keyword)
    // Task facets
    final val taskType: Facet = Facet("taskType", "Task type", "The concrete type of a task.", FacetType.keyword)
    // Generic facets
    final val createdBy: Facet = Facet("createdBy", "Created by", "The user who created the item.", FacetType.keyword)
    final val lastModifiedBy: Facet = Facet("lastModifiedBy", "Last modified by", "The user who last modified the item.", FacetType.keyword)
    final val tags: Facet = Facet("tags", "Tags", "The user-defined tags.", FacetType.keyword)
    // Activity facets
    final val activityStatus: Facet = Facet("status", "Status", "The activity status.", FacetType.keyword)
    final val activityType: Facet = Facet("activityType", "Activity type", "Activity type (either cache or non-cache activity).", FacetType.keyword)
    final val activityStartedBy: Facet = Facet("startedBy", "Started by", "The user that started the activity", FacetType.keyword)

    val facetIds: Seq[String] = Seq(datasetType, fileResource, taskType, transformInputResource, workflowExecutionStatus,
      createdBy, lastModifiedBy, tags, activityStatus, activityType, activityStartedBy).map(_.id)
    assert(facetIds.distinct.size == facetIds.size, "Facet IDs must be unique!")
  }

  /** The property of the search item to sort by and the label to display in the UI. */
  case class SortableProperty(id: String, label: String)
  lazy implicit val sortablePropertyWrites: Format[SortableProperty] = Json.format[SortableProperty]

  /** The result of a faceted search. */
  @Schema(description = "The result list as well as the list of potential facets for the currently selected task type.")
  case class FacetedSearchResult(total: Int,
                                 @ArraySchema(schema = new Schema(implementation = classOf[Object]))
                                 results: Seq[JsObject],
                                 @ArraySchema(schema = new Schema(implementation = classOf[SortableProperty]))
                                 sortByProperties: Seq[SortableProperty],
                                 @ArraySchema(schema = new Schema(implementation = classOf[FacetResult]))
                                 facets: Seq[FacetResult])

  lazy implicit val facetedSearchResult: Format[FacetedSearchResult] = Json.format[FacetedSearchResult]

  type ProjectOrTask = Either[(ProjectTask[_ <: TaskSpec], TypedTasks), Project]

  /** Tasks of a specific item type, e.g. dataset, transform, workflow... */
  case class TypedTasks(project: String,
                        projectLabel: String,
                        itemType: ItemType,
                        tasks: Seq[ProjectTask[_ <: TaskSpec]])

  object TypedTasks {

    /** Fetches tasks. If the item type is defined, it will only fetch tasks of a specific type. */
    def fetchTasks(project: Project, itemType: Option[ItemType])
                          (implicit userContext: UserContext): Seq[TypedTasks] = {
      itemType match {
        case Some(t) =>
          Seq(fetchTasksOfType(project, t))
        case None =>
          val result = new ArrayBuffer[TypedTasks]()
          for (it <- ItemType.taskTypes) {
            result.append(fetchTasksOfType(project, it))
          }
          result.toSeq
      }
    }

    /** Fetch tasks of a specific type. */
    private def fetchTasksOfType(project: Project, itemType: ItemType)
                                (implicit userContext: UserContext): TypedTasks = {
      val tasks = itemType match {
        case ItemType.dataset => project.tasks[DatasetSpec[Dataset]]
        case ItemType.linking => project.tasks[LinkSpec]
        case ItemType.transform => project.tasks[TransformSpec]
        case ItemType.workflow => project.tasks[Workflow]
        case ItemType.task => project.tasks[CustomTask]
        case ItemType.project => Seq.empty
        case ItemType.global => Seq.empty
      }
      TypedTasks(project.id, project.config.fullLabel ,itemType, tasks)
    }
  }

  /** A search request that supports types and facets. */
  case class FacetedSearchRequest(@Schema(
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
                                    allowableValues = Array("project", "dataset", "transform", "linking", "workflow", "task")
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
                                  facets: Option[Seq[FacetSetting]] = None,
                                  @Schema(
                                    description = "If set to true, the current configuration for each task item is returned in the search response.",
                                    required = false,
                                    nullable = true
                                  )
                                  addTaskParameters: Option[Boolean] = Some(false)) extends SearchRequestTrait {
    /** The offset used for paging. */
    def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

    /** The limit used for paging. */
    def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

    /** Execute search request and return result list. */
    def apply()(implicit userContext: UserContext,
                accessMonitor: WorkbenchAccessMonitor): JsValue = {
      val ps: Seq[Project] = projects
      var tasks: Seq[TypedTasks] = ps.flatMap(TypedTasks.fetchTasks(_, itemType))
      var selectedProjects: Seq[Project] = if(project.isEmpty && (itemType.contains(ItemType.project) || itemType.isEmpty)) ps else Seq()

      for(term <- textQuery if term.trim.nonEmpty) {
        val lowerCaseTerm = extractSearchTerms(term)
        tasks = tasks.map(typedTasks => filterTasksByTextQuery(typedTasks, lowerCaseTerm))
        selectedProjects = if(itemType.contains(ItemType.project) || itemType.isEmpty) selectedProjects.filter(p => matchesSearchTerm(lowerCaseTerm, p)) else Seq()
      }

      // facets are collected after filtering, so only non empty facets are displayed with correct counts
      val overallFacetCollector = OverallFacetCollector()
      val facetSettings = facets.getOrElse(Seq.empty)
      tasks = tasks.map(t => filterTasksByFacetSettings(t, overallFacetCollector, facetSettings))
      selectedProjects = selectedProjects.filter(p => overallFacetCollector.filterAndCollectProjects(p, facetSettings))
      val tasksWithTypedTask: Seq[ProjectOrTask] = tasks.flatMap(typedTasks =>
        typedTasks.tasks.map(typedTask => Left((typedTask, typedTasks))))
      val selectProjectsEither: Seq[ProjectOrTask] = selectedProjects.map(Right.apply)
      val projectsAndTasks: Seq[ProjectOrTask] = selectProjectsEither ++ tasksWithTypedTask
      val sorted = sort(projectsAndTasks)
      val resultWindow =
        if(workingLimit != 0) {
          sorted.slice(workingOffset, workingOffset + workingLimit)
        } else {
          sorted.drop(workingOffset)
        }
      val resultWindowJson = resultWindow map {
        case left @ Left((task, typedTasks)) => (toJson(task, typedTasks), left)
        case right @ Right(project) => (toJson(project), right)
      }
      val withItemLinks = addItemLinks(resultWindowJson)

      val facetResults = overallFacetCollector.results

      Json.toJson(FacetedSearchResult(
        total = sorted.size,
        results = withItemLinks,
        sortByProperties = Seq(SortableProperty("label", "Label")),
        facets = facetResults.toSeq
      ))
    }

    // Sort results according to request
    private def sort(projectOrTasks: Seq[ProjectOrTask])
                    (implicit accessMonitor: WorkbenchAccessMonitor,
                     userContext: UserContext): Seq[ProjectOrTask] = {
      sortBy match {
        case None =>
          sortByMostRecentlyViewed(projectOrTasks)
        case Some(by) =>
          val sortAsc = !sortOrder.contains(SortOrder.DESC)
          val fetchValue: ProjectOrTask => String = sortValueFunction(by)
          val sortFunction: (String, String) => Boolean = createSearchFunction(sortAsc)
          projectOrTasks.sortWith((left, right) => sortFunction(fetchValue(left), fetchValue(right)))
      }
    }

    // Sorts the result list by most recently viewed items of the user
    private def sortByMostRecentlyViewed(jsonResult: Seq[ProjectOrTask])
                                        (implicit accessMonitor: WorkbenchAccessMonitor,
                                         userContext: UserContext): Seq[ProjectOrTask] = {
      val userAccessItems = accessMonitor.getAccessItems.reverse // last item is the most recent item, so reverse
      val userAccessItemSet = userAccessItems.toSet
      val (recentlyViewed, others) = jsonResult.partition(projectOrTask => userAccessItemSet.contains(toWorkspaceItem(projectOrTask)))
      val recentlyViewedSorted = {
        val resultMap = recentlyViewed.map(projectOrTask => toWorkspaceItem(projectOrTask) -> projectOrTask).toMap
        for (userAccessItem <- userAccessItems if resultMap.contains(userAccessItem)) yield {
          resultMap(userAccessItem)
        }
      }
      recentlyViewedSorted ++ others
    }

    private def toWorkspaceItem(projectOrTask: ProjectOrTask)
                                 (implicit accessMonitor: WorkbenchAccessMonitor): WorkspaceItem = {
      projectOrTask match {
        case Right(project) =>
          WorkspaceProject(project.id)
        case Left((projectTask, _)) =>
          WorkspaceTask(projectTask.project.id, projectTask.id)
      }
    }

    // Adds links to related pages to the result item
    private def addItemLinks(results: Seq[(JsObject, ProjectOrTask)]): Seq[JsObject] = {
      results map { case (resultJson, projectOrTask) =>
        val project = jsonPropertyStringValue(resultJson, PROJECT_ID)
        val itemId = jsonPropertyStringValue(resultJson, ID)
        val links: Seq[ItemLink] = ItemType.itemTypeFormat.reads(resultJson.value(TYPE)).asOpt match {
          case Some(itemType) =>
            ItemType.itemTypeLinks(itemType, project, itemId, projectOrTask.left.toOption.map(_._1.data))
          case None =>
            Seq.empty
        }
        resultJson + ("itemLinks" -> JsArray(links.map(ItemLink.itemLinkFormat.writes)))
      }
    }

    private def jsonPropertyStringValue(result: JsObject, property: String): String = {
      result.value.get(property).map(_.as[String]).getOrElse("")
    }

    private def filterTasksByTextQuery(typedTasks: TypedTasks,
                                       lowerCaseTerms: Iterable[String])(implicit userContext: UserContext): TypedTasks = {
      typedTasks.copy(tasks = typedTasks.tasks.filter { task =>
          // Project is shown in search results when not restricting by project. Task properties are not shown.
        matchesSearchTerm(lowerCaseTerms, task, matchTaskProperties = false, matchProject = project.isEmpty) })
    }

    private def filterTasksByFacetSettings(typedTasks: TypedTasks,
                                           facetCollector: OverallFacetCollector,
                                           facetSettings: Seq[FacetSetting])
                                          (implicit user: UserContext): TypedTasks = {
      itemType match {
        case Some(typ) if typedTasks.itemType == typ =>
          typedTasks.copy(tasks = typedTasks.tasks.filter { task => facetCollector.filterAndCollectByItemType(typ, task, facetSettings) })
        case _ =>
          typedTasks.copy(tasks = typedTasks.tasks.filter { task => facetCollector.filterAndCollectAllItems(task, facetSettings)})
      }
    }

    private def toJson(project: Project)
                      (implicit userContext: UserContext): JsObject = {
      JsObject(
        Seq(
          TYPE -> JsString(PROJECT_TYPE),
          ID -> JsString(project.config.id),
          LABEL -> JsString(label(project)),
          TAGS -> Json.toJson(project.tags().map(FullTag.fromTag))
        ) ++ project.config.metaData.description.toSeq.map { desc =>
          DESCRIPTION -> JsString(desc)
        }
      )
    }

    private val addParameters = addTaskParameters.getOrElse(false)

    private def toJson(task: ProjectTask[_ <: TaskSpec],
                       typedTask: TypedTasks)(implicit userContext: UserContext): JsObject = {
      val pd = PluginDescription.forTask(task)
      val parameters = if(addParameters) {
        implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(task.project)
        val jsonValue = TaskSpecJsonFormat.write(task.data)
        Seq(PARAMETERS -> (jsonValue \ "parameters").as[JsObject])
      } else {
        Seq.empty
      }
      val datasetAttributes = {
        task.data match {
          case ds: GenericDatasetSpec =>
            Seq(READ_ONLY -> JsBoolean(ds.readOnly)) ++
              ds.uriAttribute.map(uri => URI_PROPERTY -> JsString(uri))
          case _ =>
            Seq.empty
        }
      }
      JsObject(
        Seq(
          PROJECT_ID -> JsString(typedTask.project),
          PROJECT_LABEL -> JsString(typedTask.projectLabel),
          TYPE -> JsString(typedTask.itemType.id),
          ID -> JsString(task.id),
          LABEL -> JsString(label(task)),
          DESCRIPTION -> JsString(""),
          PLUGIN_ID -> JsString(pd.id),
          PLUGIN_LABEL -> JsString(pd.label),
          TAGS -> Json.toJson(task.tags().map(FullTag.fromTag))
        )
          ++ task.metaData.description.map(d => DESCRIPTION -> JsString(d))
          ++ parameters
          ++ datasetAttributes
      )
    }
  }

  private def label(project: Project): String = {
    project.config.fullLabel
  }

  private def label(task: ProjectTask[_ <: TaskSpec]): String = {
    task.fullLabel
  }

  private def label(projectOrTask: ProjectOrTask): String = {
    projectOrTask match {
      case Left((task, _)) => label(task)
      case Right(project) => label(project)
    }
  }

  /** Function that extracts a value from the JSON object. */
  private def sortValueFunction(by: SortBy.Value): ProjectOrTask => String = {
    // memorize converted values in order to not recompute
    val valueMapping = mutable.HashMap[ProjectOrTask, String]()
    projectOrTask: ProjectOrTask => {
      valueMapping.getOrElseUpdate(
        projectOrTask,
        by match {
          case SortBy.label => label(projectOrTask).toLowerCase
        }
      )
    }
  }

  private def createSearchFunction(sortAsc: Boolean): (String, String) => Boolean = {
    if (sortAsc) {
      (left: String, right: String) => left < right
    } else {
      (left: String, right: String) => left > right
    }
  }

  /** A simple text based search request. The text query has to match as a whole. */
  case class SearchRequest(@Schema(
                             description = "Restrict search to a specific project.",
                             nullable = true,
                             required = false)
                           project: Option[String],
                           @Schema(
                             description = "Only return tasks that match a search term. Currently, the search covers the ID, the label, the description and the task properties.",
                             nullable = true,
                             required = false)
                           searchTerm: Option[String],
                           @Schema(
                             description = "The format options specify which parts are to be included in the response.",
                             nullable = true,
                             required = false)
                           formatOptions: Option[TaskFormatOptions]) extends SearchRequestTrait {

    // JSON format to serialize tasks according to the options
    private def taskFormat(userContext: UserContext): TaskJsonFormat[TaskSpec] = {
      new TaskJsonFormat[TaskSpec](formatOptions.getOrElse(TaskFormatOptions()), Some(userContext))
    }

    /**
      * Executes the search request and generates the JSON response.
      */
    def apply()(implicit userContext: UserContext): JsValue = {
      var tasks = projects.flatMap(_.allTasks)

      for(term <- searchTerm) {
        val lowerCaseTerm = extractSearchTerms(term)
        tasks = tasks.filter(task => matchesSearchTerm(lowerCaseTerm, task, matchTaskProperties = true, matchProject = false))
      }

      JsArray(tasks.map(writeTask))
    }

    private def writeTask(task: ProjectTask[_ <: TaskSpec])
                         (implicit userContext: UserContext): JsValue = {
      taskFormat(userContext).write(task)(WriteContext[JsValue](prefixes = task.project.config.prefixes, projectId = Some(task.project.id),
        resources = task.project.resources))
    }
  }
}
