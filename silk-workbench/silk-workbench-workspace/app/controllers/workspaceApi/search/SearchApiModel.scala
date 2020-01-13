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

/**
  * Data structures used for handling search requests
  */
object SearchApiModel {
  // Property names
  final val LABEL = "label"
  final val ID = "id"
  final val TYPE = "type"
  final val DESCRIPTION = "description"
  final val PROJECT_ID = "projectId"
  // type values
  final val PROJECT_TYPE = "project"
  /* JSON serialization */
  implicit val responseOptionsReader: Reads[TaskFormatOptions] = Json.reads[TaskFormatOptions]
  implicit val searchRequestReader: Reads[SearchRequest] = Json.reads[SearchRequest]
  implicit val itemTypeReads: Format[ItemType] = new Format[ItemType] {
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
  implicit val sortOrderReads: Reads[SortOrder.Value] = Reads.enumNameReads(SortOrder)
  implicit val sortByReads: Reads[SortBy.Value] = Reads.enumNameReads(SortBy)
  implicit val facetTypesReads: Reads[FacetType.Value] = Reads.enumNameReads(FacetType)
  implicit val facetSettingReads: Reads[FacetSetting] = new Reads[FacetSetting] {
    override def reads(json: JsValue): JsResult[FacetSetting] = {
      (json \ TYPE).toOption.map(_.as[String]) match {
        case Some(facetType) if FacetType.keyword.toString == facetType => KeywordFacetSetting.keywordFacetSettingReads.reads(json)
        case Some(invalidType) => throw BadUserInputException("No valid facet type specified: '" + invalidType + "'. Valid values are: " +
            FacetType.facetTypeSet.mkString(", "))
        case None => throw BadUserInputException("No 'type' property found in given JSON: " + json.toString())
      }
    }
  }
  implicit val facetedSearchRequestReader: Reads[FacetedSearchRequest] = Json.reads[FacetedSearchRequest]
  val keywordFacetValues: Writes[KeywordFacetValues] = new Writes[KeywordFacetValues] {
    override def writes(keywordFacetValues: KeywordFacetValues): JsValue = {
      val values = keywordFacetValues.values.map(v => JsObject(Seq(
        ID -> JsString(v.id),
        LABEL -> JsString(v.label)
      ) ++ v.count.map(c => "count" -> JsNumber(c))))
      JsArray(values)
    }
  }
  implicit val facetValuesWrites: Writes[FacetValues] = new Writes[FacetValues] {
    override def writes(o: FacetValues): JsValue = {
      o match {
        case kw: KeywordFacetValues => keywordFacetValues.writes(kw)
      }
    }
  }
  implicit val facetResultWrites: Writes[FacetResult] = Json.writes[FacetResult]

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
    val keywordFacetSettingReads: Reads[KeywordFacetSetting] = Json.reads[KeywordFacetSetting]
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

    protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], project: Project): Boolean = {
      val idMatch = matchesSearchTerm(lowerCaseSearchTerms, project.config.id)
      val nameMatch = matchesSearchTerm(lowerCaseSearchTerms, project.name)
      idMatch || nameMatch
    }

    protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], searchIn: String): Boolean = {
      val lowerCaseText = searchIn.toLowerCase
      lowerCaseSearchTerms forall lowerCaseText.contains
    }
  }

  case class Facet(id: String, label: String, description: String, facetType: FacetType.Value)

  object Facets {
    // dataset facets
    final val datasetType: Facet = Facet("datasetType", "Dataset type", "The concrete type of a dataset, e.g. its data model and format etc.", FacetType.keyword)
    final val fileResource: Facet = Facet("datasetFileResource", "Dataset file", "The file resource of a file based dataset.", FacetType.keyword)

    val facetIds: Seq[String] = Seq(datasetType, fileResource).map(_.id)
  }

  /** A search request that supports types and facets. */
  case class FacetedSearchRequest(project: Option[String],
                                  itemType: Option[ItemType],
                                  textQuery: Option[String],
                                  offset: Option[Int],
                                  limit: Option[Int],
                                  sortBy: Option[SortBy.Value],
                                  sortOrder: Option[SortOrder.Value],
                                  facets: Option[Seq[FacetSetting]]) extends SearchRequestTrait {
    def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

    def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

    def apply()(implicit userContext: UserContext,
                accessMonitor: WorkbenchAccessMonitor): JsValue = {
      val ps: Seq[Project] = projects
      var tasks: Seq[TypedTasks] = ps.flatMap(fetchTasks)
      var selectedProjects: Seq[Project] = Seq.empty

      for(term <- textQuery) {
        val lowerCaseTerm = extractSearchTerms(term)
        tasks = tasks.map(typedTasks => filterTasksByTextQuery(typedTasks, lowerCaseTerm))
        selectedProjects = if(itemType.contains(ItemType.project)) ps.filter(p => matchesSearchTerm(lowerCaseTerm, p)) else Seq()
      }

      // facets are collected after filtering, so only non empty facets are displayed with correct counts
      val facetCollectors = OverallFacetCollector()
      collectFacetValues(tasks, facetCollectors)
      val facets = facetCollectors.results
      tasks = tasks.map(t => filterTasksByFacetSettings(t))
      val jsonResult = selectedProjects.map(toJson) ++ tasks.flatMap(toJson)
      val sorted = sort(jsonResult)
      val resultWindow = sorted.slice(workingOffset, workingOffset + workingLimit)
      val withItemLinks = addItemLinks(resultWindow)

      JsObject(Seq(
        "total" -> JsNumber(BigDecimal(sorted.size)),
        "results" -> JsArray(withItemLinks),
        "sortByProperties" -> JsArray(Seq(SortableProperty("label", "Label")).map(sortablePropertyWrites.writes)),
        "facets" -> JsArray(facets.map(fr => Json.toJson(fr)).toSeq)
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
      val labelMatch = matchesSearchTerm(lowerCaseSearchTerms, name)
      val descriptionMatch = matchesSearchTerm(lowerCaseSearchTerms, task.metaData.description.getOrElse(""))
      labelMatch || descriptionMatch
    }

    /** Collects the result facet values. */
    def resultFacets(tasks: Seq[TypedTasks]): Seq[FacetResult] = {
      val datasetFacetCollector = DatasetFacetCollector()
      for(typedTasks <- tasks) {
        typedTasks.itemType match {
          case ItemType.dataset =>
            if(itemType.contains(ItemType.dataset)) {
              typedTasks.tasks foreach { task =>
                datasetFacetCollector.collect(task.asInstanceOf[ProjectTask[DatasetSpec[Dataset]]])
              }
            }
          case _ =>
            // No other facet collectors defined
            Seq.empty
        }
      }
      datasetFacetCollector.result
    }

    // Adds links to related pages to the result item
    private def addItemLinks(results: Seq[JsObject]): Seq[JsObject] = {
      results map { result =>
        val project = jsonPropertyStringValue(result, PROJECT_ID)
        val itemId = jsonPropertyStringValue(result, ID)
        val context = WorkbenchConfig.applicationContext
        val links: Seq[ItemLink] = itemTypeReads.reads(result.value(TYPE)).asOpt match {
          case Some(itemType) =>
            itemType match {
              case ItemType.transform => Seq(
                ItemLink("Mapping editor", s"$context/transform/$project/$itemId/editor"),
                ItemLink("Transform evaluation", s"$context/transform/$project/$itemId/evaluate"),
                ItemLink("Transform execution", s"$context/transform/$project/$itemId/execute")
              )
              case ItemType.linking => Seq(
                ItemLink("Linking editor", s"$context/linking/$project/$itemId/editor"),
                ItemLink("Linking evaluation", s"$context/linking/$project/$itemId/evaluate"),
                ItemLink("Linking execution", s"$context/linking/$project/$itemId/execute")
              )
              case ItemType.workflow => Seq(
                ItemLink("Workflow editor", s"$context/workflow/editor/$project/$itemId")
              )
              case _ => Seq.empty
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

    case class SortableProperty(id: String, label: String)
    implicit val sortablePropertyWrites: Writes[SortableProperty] = Json.writes[SortableProperty]

    private def collectFacetValues(tasks: Seq[TypedTasks], facetCollectors: OverallFacetCollector): Unit = {
      // Only collect facets for specific item types. FIXME: Add generic collector, e.g. for creation date, creator etc.
      for (typedTasks <- tasks if itemType.contains(typedTasks.itemType)) {
        for (task <- typedTasks.tasks) {
          facetCollectors.collect(typedTasks.itemType, task)
        }
      }
    }

    private def filterTasksByTextQuery(typedTasks: TypedTasks,
                                       lowerCaseTerms: Seq[String]): TypedTasks = {
      typedTasks.copy(tasks = typedTasks.tasks.filter { task => matchesSearchTerm(lowerCaseTerms, task) })
    }

    private def filterTasksByFacetSettings(typedTasks: TypedTasks): TypedTasks = {
      val facetMatchingFN = matchesFacetSettingFunction(typedTasks.itemType)
      typedTasks.copy(tasks = typedTasks.tasks.filter { task => facetMatchingFN(task) })
    }

    /** Returns a function to check if a task matches the facet filter setting */
    private def matchesFacetSettingFunction(itemType: ItemType): ProjectTask[_ <: TaskSpec] => Boolean = {
      val alwaysTrueFunction: ProjectTask[_ <: TaskSpec] => Boolean = _ => true
      facets match {
        case None => alwaysTrueFunction
        case Some(facetSettings) if facetSettings.isEmpty => alwaysTrueFunction
        case Some(facetSettings) =>
          itemType match { // FIXME: Improve facet filtering code when more facets are added
            case ItemType.dataset =>
              facetSettings.find(_.facetId == Facets.datasetType.id) match {
                case Some(datasetType: KeywordFacetSetting) =>
                  val datasetTypeIds = datasetType.keywordIds
                  task: ProjectTask[_ <: TaskSpec] => {
                    datasetTypeIds.contains(task.asInstanceOf[ProjectTask[DatasetSpec[Dataset]]].data.plugin.pluginSpec.id)
                  }
                case None => alwaysTrueFunction
              }
            case _ =>
              alwaysTrueFunction
          }
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
      JsObject(Seq(
        TYPE -> JsString(PROJECT_TYPE),
        ID -> JsString(project.config.id),
        LABEL -> JsString(project.config.id),// TODO: Support label and description in projects
        DESCRIPTION -> JsString("")
      ))
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

  private def extractSearchTerms(term: String) = {
    term.toLowerCase.split("\\s+").filter(_.nonEmpty)
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
