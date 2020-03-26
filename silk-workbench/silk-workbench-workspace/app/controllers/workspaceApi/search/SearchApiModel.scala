package controllers.workspaceApi.search

import config.WorkbenchConfig
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers.{TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.workbench.workspace.{WorkbenchAccessMonitor, WorkspaceItem, WorkspaceProject, WorkspaceTask}
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

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
  // type values
  final val PROJECT_TYPE = "project"
  /* JSON serialization */
  lazy implicit val responseOptionsReader: Reads[TaskFormatOptions] = Json.reads[TaskFormatOptions]
  lazy implicit val searchRequestReader: Reads[SearchRequest] = Json.reads[SearchRequest]
  lazy implicit val itemTypeReads: Format[ItemType] = new Format[ItemType] {
    override def reads(json: JsValue): JsResult[ItemType] = {
      json match {
        case JsString(value) =>ItemType.idToItemType.get(value) match {
          case Some(itemType) => JsSuccess(itemType)
          case None => throw BadUserInputException(s"Invalid value for itemType. Got '$value'. Value values: " + ItemType.ordered.map(_.id).mkString(", "))
        }
        case _ => throw BadUserInputException("Invalid value for itemType. String value expected.")
      }
    }
    override def writes(o: ItemType): JsValue = JsString(o.id)
  }
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
  lazy implicit val facetedSearchRequestReader: Reads[FacetedSearchRequest] = Json.reads[FacetedSearchRequest]
  lazy implicit val keywordFacetValueReads: Format[KeywordFacetValue] = Json.format[KeywordFacetValue]
  lazy implicit val facetResultWrites: Writes[FacetResult] = new Writes[FacetResult] {
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
  }

  /** The item types the search can be restricted to. */
  sealed abstract class ItemType(val id: String, val label: String)

  object ItemType {
    case object project extends ItemType(PROJECT_TYPE, "Project")
    case object dataset extends ItemType("dataset", "Dataset")
    case object transform extends ItemType("transform", "Transform")
    case object linking extends ItemType("linking", "Linking")
    case object workflow extends ItemType("workflow", "Workflow")
    case object task extends ItemType("task", "Task")

    val ordered: Seq[ItemType] = Seq(project, workflow, dataset, transform, linking, task)
    val idToItemType: Map[String, ItemType] = ordered.map(it => (it.id, it)).toMap
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

    /**
      * Checks if a task matches the search term.
      */
    protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], task: ProjectTask[_ <: TaskSpec]): Boolean = {
      val idMatch = matchesSearchTerm(lowerCaseSearchTerms, task.id)
      val labelMatch = matchesSearchTerm(lowerCaseSearchTerms, task.metaData.label)
      val descriptionMatch = matchesSearchTerm(lowerCaseSearchTerms, task.metaData.description.getOrElse(""))
      val propertiesMatch = task.data.properties(task.project.config.prefixes).exists(p => matchesSearchTerm(lowerCaseSearchTerms, p._2))
      idMatch || labelMatch || descriptionMatch || propertiesMatch
    }

    /** Split text query into multi term search */
    protected def extractSearchTerms(term: String): Array[String] = {
      term.toLowerCase.split("\\s+").filter(_.nonEmpty)
    }

    /** Match search terms against project. */
    protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], project: Project): Boolean = {
      val idMatch = matchesSearchTerm(lowerCaseSearchTerms, project.config.id)
      val labelMatch = matchesSearchTerm(lowerCaseSearchTerms, project.config.metaData.label)
      val descriptionMatch = project.config.metaData.description.exists(d => matchesSearchTerm(lowerCaseSearchTerms, d))
      idMatch || labelMatch || descriptionMatch
    }

    /** Match search terms against string. Returns only true if all search terms match at least one of the provided strings. */
    protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], searchIn: String*): Boolean = {
      val lowerCaseTexts = searchIn.map(_.toLowerCase)
      lowerCaseSearchTerms forall (searchTerm => lowerCaseTexts.exists(_.contains(searchTerm)))
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

    val facetIds: Seq[String] = Seq(datasetType, fileResource, taskType, transformInputResource, workflowExecutionStatus, createdBy, lastModifiedBy).map(_.id)
    assert(facetIds.distinct.size == facetIds.size, "Facet IDs must be unique!")
  }

  /** The property of the search item to sort by and the label to display in the UI. */
  case class SortableProperty(id: String, label: String)
  lazy implicit val sortablePropertyWrites: Writes[SortableProperty] = Json.writes[SortableProperty]

  /** The result of a faceted search. */
  case class FacetedSearchResult(total: Int,
                                 results: Seq[JsObject],
                                 sortByProperties: Seq[SortableProperty],
                                 facets: Seq[FacetResult])

  lazy implicit val facetedSearchResult: Writes[FacetedSearchResult] = Json.writes[FacetedSearchResult]

  /** A search request that supports types and facets. */
  case class FacetedSearchRequest(project: Option[String] = None,
                                  itemType: Option[ItemType] = None,
                                  textQuery: Option[String] = None,
                                  offset: Option[Int] = None,
                                  limit: Option[Int] = None,
                                  sortBy: Option[SortBy.Value] = None,
                                  sortOrder: Option[SortOrder.Value] = None,
                                  facets: Option[Seq[FacetSetting]] = None) extends SearchRequestTrait {
    /** The offset used for paging. */
    def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

    /** The limit used for paging. */
    def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

    /** Execute search request and return result list. */
    def apply()(implicit userContext: UserContext,
                accessMonitor: WorkbenchAccessMonitor): JsValue = {
      val ps: Seq[Project] = projects
      var tasks: Seq[TypedTasks] = ps.flatMap(fetchTasks)
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
      val jsonResult = selectedProjects.map(toJson) ++ tasks.flatMap(toJson)
      val sorted = sort(jsonResult)
      val resultWindow = sorted.slice(workingOffset, workingOffset + workingLimit)
      val withItemLinks = addItemLinks(resultWindow)
      val facetResults = overallFacetCollector.results

      Json.toJson(FacetedSearchResult(
        total = sorted.size,
        results = withItemLinks,
        sortByProperties = Seq(SortableProperty("label", "Label")),
        facets = facetResults.toSeq
      ))
    }

    // Sort results according to request
    private def sort(jsonResult: Seq[JsObject])
                    (implicit accessMonitor: WorkbenchAccessMonitor,
                     userContext: UserContext): Seq[JsObject] = {
      sortBy match {
        case None =>
          sortByMostRecentlyViewed(jsonResult)
        case Some(by) =>
          val sortAsc = !sortOrder.contains(SortOrder.DESC)
          val fetchValue: JsObject => String = sortValueFunction(by)
          val sortFunction: (String, String) => Boolean = createSearchFunction(sortAsc)
          jsonResult.sortWith((left, right) => sortFunction(fetchValue(left), fetchValue(right)))
      }
    }

    // Sorts the result list by most recently viewed items of the user
    private def sortByMostRecentlyViewed(jsonResult: Seq[JsObject])
                                        (implicit accessMonitor: WorkbenchAccessMonitor,
                                         userContext: UserContext): Seq[JsObject] = {
      val userAccessItems = accessMonitor.getAccessItems.reverse // last item is the most recent item, so reverse
      val userAccessItemSet = userAccessItems.toSet
      val (recentlyViewed, others) = jsonResult.partition(jsObject => userAccessItemSet.contains(jsToWorkspaceItem(jsObject)))
      val recentlyViewedSorted = {
        val resultMap = recentlyViewed.map(jsObj => jsToWorkspaceItem(jsObj) -> jsObj).toMap
        for (userAccessItem <- userAccessItems if resultMap.contains(userAccessItem)) yield {
          resultMap(userAccessItem)
        }
      }
      recentlyViewedSorted ++ others
    }

    private def jsToWorkspaceItem(jsObject: JsObject)
                                 (implicit accessMonitor: WorkbenchAccessMonitor): WorkspaceItem = {
      jsObject.value(TYPE).as[String] match {
        case PROJECT_TYPE =>
          WorkspaceProject(jsObject.value(ID).as[String])
        case _ =>
          WorkspaceTask(jsObject.value(PROJECT_ID).as[String], jsObject.value(ID).as[String])
      }
    }

    override protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], task: ProjectTask[_ <: TaskSpec]): Boolean = {
      val taskLabel = task.metaData.label
      val name = if(taskLabel.trim != "") taskLabel else task.id.toString
      matchesSearchTerm(lowerCaseSearchTerms, name, task.metaData.description.getOrElse(""))
    }

    // TODO: Update URL after deciding on path for new workspace
    private def workspaceProjectPath(projectId: String) = s"workspaceNew/projects/$projectId"
    // Adds links to related pages to the result item
    private def addItemLinks(results: Seq[JsObject]): Seq[JsObject] = {

      results map { result =>
        val project = jsonPropertyStringValue(result, PROJECT_ID)
        val itemId = jsonPropertyStringValue(result, ID)
        val context = WorkbenchConfig.applicationContext
        val detailsPageBase = s"$context/${workspaceProjectPath(project)}"
        val links: Seq[ItemLink] = itemTypeReads.reads(result.value(TYPE)).asOpt match {
          case Some(itemType) =>
            itemType match {
              case ItemType.dataset => Seq(
                ItemLink("Dataset details page", s"$detailsPageBase/${ItemType.dataset.id}/$itemId")
              )
              case ItemType.transform => Seq(
                ItemLink("Transform details page", s"$detailsPageBase/${ItemType.transform.id}/$itemId"),
                ItemLink("Mapping editor", s"$context/transform/$project/$itemId/editor"),
                ItemLink("Transform evaluation", s"$context/transform/$project/$itemId/evaluate"),
                ItemLink("Transform execution", s"$context/transform/$project/$itemId/execute")
              )
              case ItemType.linking => Seq(
                ItemLink("Linking details page", s"$detailsPageBase/${ItemType.linking.id}/$itemId"),
                ItemLink("Linking editor", s"$context/linking/$project/$itemId/editor"),
                ItemLink("Linking evaluation", s"$context/linking/$project/$itemId/evaluate"),
                ItemLink("Linking execution", s"$context/linking/$project/$itemId/execute")
              )
              case ItemType.workflow => Seq(
                ItemLink("Workflow details page", s"$detailsPageBase/${ItemType.workflow.id}/$itemId"),
                ItemLink("Workflow editor", s"$context/workflow/editor/$project/$itemId")
              )
              case ItemType.task => Seq(
                ItemLink("Task details page", s"$detailsPageBase/${ItemType.task.id}/$itemId")
              )
              case ItemType.project => Seq(
                ItemLink("Project details page", s"$context/${workspaceProjectPath(itemId)}")
              )
            }
          case None => Seq.empty
        }
        result + ("itemLinks" -> JsArray(links.map(ItemLink.itemLinkWrites.writes)))
      }
    }

    private def jsonPropertyStringValue(result: JsObject, property: String): String = {
      result.value.get(property).map(_.as[String]).getOrElse("")
    }

    case class ItemLink(label: String, path: String)
    object ItemLink {
      val itemLinkWrites: Writes[ItemLink] = Json.writes[ItemLink]
    }

    private def filterTasksByTextQuery(typedTasks: TypedTasks,
                                       lowerCaseTerms: Seq[String]): TypedTasks = {
      typedTasks.copy(tasks = typedTasks.tasks.filter { task => matchesSearchTerm(lowerCaseTerms, task) })
    }

    private def filterTasksByFacetSettings(typedTasks: TypedTasks,
                                           facetCollector: OverallFacetCollector,
                                           facetSettings: Seq[FacetSetting]): TypedTasks = {
      itemType match {
        case Some(typ) if typedTasks.itemType == typ =>
          typedTasks.copy(tasks = typedTasks.tasks.filter { task => facetCollector.filterAndCollectByItemType(typ, task, facetSettings) })
        case _ =>
          typedTasks.copy(tasks = typedTasks.tasks.filter { task => facetCollector.filterAndCollectAllItems(task, facetSettings)})
      }
    }

    /** Fetches the tasks. If the item type is defined, it will only fetch tasks of a specific type. */
    private def fetchTasks(project: Project)
                          (implicit userContext: UserContext): Seq[TypedTasks] = {
      itemType match {
        case Some(t) =>
          Seq(fetchTasksOfType(project, t))
        case None =>
          val result = new ArrayBuffer[TypedTasks]()
          for (it <- ItemType.ordered.filter(_ != ItemType.project)) {
            result.append(fetchTasksOfType(project, it))
          }
          result
      }
    }

    /** Tasks of a specific item type, e.g. dataset, transform, workflow... */
    case class TypedTasks(project: String,
                          itemType: ItemType,
                          tasks: Seq[ProjectTask[_ <: TaskSpec]])

    /** Fetch tasks of a specific type. */
    def fetchTasksOfType(project: Project,
                         itemType: ItemType)
                        (implicit userContext: UserContext): TypedTasks = {
      val tasks = itemType match {
        case ItemType.dataset => project.tasks[DatasetSpec[Dataset]]
        case ItemType.linking => project.tasks[LinkSpec]
        case ItemType.transform => project.tasks[TransformSpec]
        case ItemType.workflow => project.tasks[Workflow]
        case ItemType.task => project.tasks[CustomTask]
        case ItemType.project => Seq.empty
      }
      TypedTasks(project.name, itemType, tasks)
    }

    private def toJson(project: Project): JsObject = {
      JsObject(
        Seq(
          TYPE -> JsString(PROJECT_TYPE),
          ID -> JsString(project.config.id),
          LABEL -> JsString(project.config.metaData.label)
        ) ++ project.config.metaData.description.toSeq.map { desc =>
          DESCRIPTION -> JsString(desc)
        }
      )
    }

    private def toJson(typedTask: TypedTasks): Seq[JsObject] = {
      typedTask.tasks map { task =>
        JsObject(Seq(
          PROJECT_ID -> JsString(typedTask.project),
          TYPE -> JsString(typedTask.itemType.id),
          ID -> JsString(task.id),
          LABEL -> JsString(task.metaData.label),
          DESCRIPTION -> JsString("")
        ) ++ task.metaData.description.map(d => DESCRIPTION -> JsString(d)))
      }
    }
  }

  /** Function that extracts a value from the JSON object. */
  private def sortValueFunction(by: SortBy.Value): JsObject => String = {
    // memorize converted values in order to not recompute
    val valueMapping = mutable.HashMap[JsObject, String]()
    jsObject: JsObject => {
      valueMapping.getOrElseUpdate(
        jsObject,
        by match {
          case SortBy.label => jsObject.value(LABEL).as[String].toLowerCase()
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
  case class SearchRequest(project: Option[String],
                           searchTerm: Option[String],
                           formatOptions: Option[TaskFormatOptions]) extends SearchRequestTrait {

    // JSON format to serialize tasks according to the options
    private def taskFormat(userContext: UserContext): TaskJsonFormat[TaskSpec] = {
      new TaskJsonFormat(formatOptions.getOrElse(TaskFormatOptions()), Some(userContext))(TaskSpecJsonFormat)
    }

    /**
      * Executes the search request and generates the JSON response.
      */
    def apply()(implicit userContext: UserContext): JsValue = {
      var tasks = projects.flatMap(_.allTasks)

      for(term <- searchTerm) {
        val lowerCaseTerm = extractSearchTerms(term)
        tasks = tasks.filter(task => matchesSearchTerm(lowerCaseTerm, task))
      }

      JsArray(tasks.map(writeTask))
    }

    private def writeTask(task: ProjectTask[_ <: TaskSpec])
                         (implicit userContext: UserContext): JsValue = {
      taskFormat(userContext).write(task)(WriteContext[JsValue](prefixes = task.project.config.prefixes, projectId = Some(task.project.name)))
    }
  }
}
