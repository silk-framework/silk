package controllers.workspaceApi.search

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.{TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, JsValue, Json, Reads, Writes}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Data structures used for handling search requests
  */
object SearchApiModel {
  implicit val responseOptionsReader: Reads[TaskFormatOptions] = Json.reads[TaskFormatOptions]
  implicit val searchRequestReader: Reads[SearchRequest] = Json.reads[SearchRequest]
  implicit val itemTypeReads: Reads[ItemType.Value] = Reads.enumNameReads(ItemType)
  implicit val sortOrderReads: Reads[SortOrder.Value] = Reads.enumNameReads(SortOrder)
  implicit val sortByReads: Reads[SortBy.Value] = Reads.enumNameReads(SortBy)
  implicit val facetSettingReads: Reads[FacetSetting] = Json.reads[FacetSetting]
  implicit val facetedSearchRequestReader: Reads[FacetedSearchRequest] = Json.reads[FacetedSearchRequest]
  val keywordFacetValues: Writes[KeywordFacetValues] = new Writes[KeywordFacetValues] {
    override def writes(keywordFacetValues: KeywordFacetValues): JsValue = {
      val values = keywordFacetValues.values.map(v => JsObject(Seq(
        "id" -> JsString(v.id),
        "label" -> JsString(v.label)
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

  object ItemType extends Enumeration {
    val Project, Dataset, Transformation, Linking, Workflow, Task = Value

    val ordered: Seq[ItemType.Value] = Seq(Project, Workflow, Dataset, Transformation, Linking, Task)
  }
  assert(ItemType.values.size == ItemType.ordered.size)

  object SortBy extends Enumeration {
    val label = Value
  }

  object SortOrder extends Enumeration {
    val ASC, DESC = Value
  }

  case class FacetResult(id: String,
                         label: String,
                         `type`: String,
                         values: FacetValues)

  /** Values of a single facet. */
  sealed trait FacetValues

  /** Values of a keyword based facet. */
  case class KeywordFacetValues(values: Seq[KeywordFacetValue]) extends FacetValues

  case class KeywordFacetValue(id: String, label: String, count: Option[Int])

  case class FacetSetting(facetType: String) // TODO: How to design different facets, e.g. data(time) range, keyword, int range etc.

  object FacetedSearchRequest {
    final val DEFAULT_OFFSET = 0
    final val DEFAULT_LIMIT = 10
    // Property names
    final val LABEL = "label"
  }

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
    protected def matchesSearchTerm(lowerCaseSearchTerm: String, task: ProjectTask[_ <: TaskSpec]): Boolean = {
      val idMatch = matchesSearchTerm(lowerCaseSearchTerm, task.id)
      val labelMatch = matchesSearchTerm(lowerCaseSearchTerm, task.metaData.label)
      val descriptionMatch = matchesSearchTerm(lowerCaseSearchTerm, task.metaData.description.getOrElse(""))
      val propertiesMatch = task.data.properties(task.project.config.prefixes).exists(p => matchesSearchTerm(lowerCaseSearchTerm, p._2))
      idMatch || labelMatch || descriptionMatch || propertiesMatch
    }

    protected def matchesSearchTerm(lowerCaseSearchTerm: String, project: Project): Boolean = {
      val idMatch = matchesSearchTerm(lowerCaseSearchTerm, project.config.id)
      val nameMatch = matchesSearchTerm(lowerCaseSearchTerm, project.name)
      idMatch || nameMatch
    }

    protected def matchesSearchTerm(lowerCaseSearchTerm: String, searchIn: String): Boolean = {
      searchIn.toLowerCase.contains(lowerCaseSearchTerm)
    }
  }

  /** A search request that supports types and facets. */
  case class FacetedSearchRequest(project: Option[String],
                                  itemType: Option[ItemType.Value],
                                  textQuery: Option[String],
                                  offset: Option[Int],
                                  limit: Option[Int],
                                  sortBy: Option[SortBy.Value],
                                  sortOrder: Option[SortOrder.Value],
                                  facets: Option[Map[String, FacetSetting]]) extends SearchRequestTrait {
    def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

    def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

    private def sort(jsonResult: Seq[JsObject]): Seq[JsObject] = {
      sortBy match {
        case None => jsonResult
        case Some(by) =>
          val sortAsc = !sortOrder.contains(SortOrder.DESC)
          val fetchValue: JsObject => String = sortValueFunction(by)
          val sortFunction: (String, String) => Boolean = createSearchFunction(sortAsc)
          jsonResult.sortWith((left, right) => sortFunction(fetchValue(left), fetchValue(right)))
      }
    }

    case class Facet(id: String, label: String)
    object Facets {
      // facets
      final val datasetType: Facet = Facet("datasetType", "Dataset type")

      // facet types
      final val KEYWORD_TYPE = "keyword"
    }

    /** Collects facet data of dataset tasks. */
    case class DatasetFacetCollector() {
      val datasetTypes = new mutable.ListMap[String, Int]()
      val datasetTypeLabel = new mutable.ListMap[String, String]()

      def collect(datasetTask: ProjectTask[DatasetSpec[Dataset]]): Unit = {
        val pluginSpec = datasetTask.plugin.pluginSpec
        val id = pluginSpec.id
        val label = pluginSpec.label
        datasetTypes.put(id, datasetTypes.getOrElseUpdate(id, 0) + 1)
        datasetTypeLabel.put(id, label)
      }

      def result: Seq[FacetResult] = {
        if(datasetTypes.nonEmpty) {
          Seq(datasetTypeFacet)
        } else {
          Seq.empty
        }
      }

      // Dataset type, e.g. CSV, JSON etc.
      private def datasetTypeFacet: FacetResult = {
        val sortedTypes = datasetTypes.toSeq.sortWith(_._2 > _._2)
        val keywordFacetValues = KeywordFacetValues(sortedTypes map (st => KeywordFacetValue(st._1, datasetTypeLabel(st._1), Some(st._2))))
        FacetResult(Facets.datasetType.id, Facets.datasetType.label, Facets.KEYWORD_TYPE, keywordFacetValues)
      }
    }

    /** Collects the result facet values. */
    def resultFacets(tasks: Seq[TypedTasks]): Seq[FacetResult] = {
      val datasetFacetCollector = DatasetFacetCollector()
      for(typedTasks <- tasks) {
        typedTasks.itemType match {
          case ItemType.Dataset =>
            typedTasks.tasks foreach { task =>
              datasetFacetCollector.collect(task.asInstanceOf[ProjectTask[DatasetSpec[Dataset]]])
            }
          case _ =>
            // No other facet collectors defined
            Seq.empty
        }
      }
      datasetFacetCollector.result
    }

    def apply()(implicit userContext: UserContext): JsValue = {
      val ps: Seq[Project] = projects
      var tasks: Seq[TypedTasks] = ps.flatMap(fetchTasks)
      var selectedProjects: Seq[Project] = Seq.empty

      for(term <- textQuery) {
        val lowerCaseTerm = term.toLowerCase
        tasks = tasks.map(typedTasks => filterTasks(typedTasks, lowerCaseTerm))
        selectedProjects = if(itemType.contains(ItemType.Project)) ps.filter(p => matchesSearchTerm(lowerCaseTerm, p)) else Seq()
      }

      // facets are collected after filtering, so only non empty facets are displayed with correct counts
      val facets = resultFacets(tasks)
      val jsonResult = selectedProjects.map(toJson) ++ tasks.flatMap(toJson)
      val sorted = sort(jsonResult)

      JsObject(Seq(
        "total" -> JsNumber(BigDecimal(tasks.size + selectedProjects.size)),
        "results" -> JsArray(sorted.slice(workingOffset, workingOffset + workingLimit)),
        "facets" -> JsArray(facets.map(fr => Json.toJson(fr)))
      ))
    }

    private def filterTasks(typedTasks: TypedTasks,
                            lowerCaseTerm: String): TypedTasks = {
      typedTasks.copy(tasks = typedTasks.tasks.filter { task => matchesSearchTerm(lowerCaseTerm, task) })
    }

    /** Fetches the tasks. If the item type is defined, it will only fetch tasks of a specific type. */
    private def fetchTasks(project: Project)
                          (implicit userContext: UserContext): Seq[TypedTasks] = {
      itemType match {
        case Some(t) =>
          Seq(fetchTasksOfType(project, t))
        case None =>
          val result = new ArrayBuffer[TypedTasks]()
          for (it <- Seq(ItemType.Dataset, ItemType.Linking, ItemType.Transformation, ItemType.Workflow, ItemType.Task)) {
            result.append(fetchTasksOfType(project, it))
          }
          result
      }
    }

    case class TypedTasks(project: String,
                          itemType: ItemType.Value,
                          tasks: Seq[ProjectTask[_ <: TaskSpec]])

    /** Fetch tasks of a specific type. */
    def fetchTasksOfType(project: Project,
                         itemType: ItemType.Value)
                        (implicit userContext: UserContext): TypedTasks = {
      val tasks = itemType match {
        case ItemType.Dataset => project.tasks[DatasetSpec[Dataset]]
        case ItemType.Linking => project.tasks[LinkSpec]
        case ItemType.Transformation => project.tasks[TransformSpec]
        case ItemType.Workflow => project.tasks[Workflow]
        case ItemType.Task => project.tasks[CustomTask]
        case ItemType.Project => Seq.empty
      }
      TypedTasks(project.name, itemType, tasks)
    }

    private def toJson(project: Project): JsObject = {
      JsObject(Seq(
        "type" -> JsString("project"),
        "id" -> JsString(project.config.id),
        FacetedSearchRequest.LABEL -> JsString(project.config.id),// TODO: Support label and description in projects
        "description" -> JsString("")
      ))
    }

    private def toJson(typedTask: TypedTasks): Seq[JsObject] = {
      typedTask.tasks map { task =>
        JsObject(Seq(
          "projectId" -> JsString(typedTask.project),
          "type" -> JsString(typedTask.itemType.toString),
          "id" -> JsString(task.id),
          "label" -> JsString(task.metaData.label),
          "description" -> JsString("")
        ) ++ task.metaData.description.map(d => "description" -> JsString(d)))
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
          case SortBy.label => jsObject.value(FacetedSearchRequest.LABEL).as[String].toLowerCase()
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
        val lowerCaseTerm = term.toLowerCase
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
