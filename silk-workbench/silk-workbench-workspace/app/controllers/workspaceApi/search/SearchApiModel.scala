package controllers.workspaceApi.search

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers.{TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat}
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Data structures used for handling search requests
  */
object SearchApiModel {
  /* JSON serialization */
  implicit val responseOptionsReader: Reads[TaskFormatOptions] = Json.reads[TaskFormatOptions]
  implicit val searchRequestReader: Reads[SearchRequest] = Json.reads[SearchRequest]
  implicit val itemTypeReads: Reads[ItemType.Value] = Reads.enumNameReads(ItemType)
  implicit val sortOrderReads: Reads[SortOrder.Value] = Reads.enumNameReads(SortOrder)
  implicit val sortByReads: Reads[SortBy.Value] = Reads.enumNameReads(SortBy)
  implicit val facetTypesReads: Reads[FacetType.Value] = Reads.enumNameReads(FacetType)
  implicit val facetSettingReads: Reads[FacetSetting] = new Reads[FacetSetting] {
    override def reads(json: JsValue): JsResult[FacetSetting] = {
      (json \ "type").toOption.map(_.as[String]) match {
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

  /** The item types the search can be restricted to. */
  object ItemType extends Enumeration {
    val Project, Dataset, Transform, Linking, Workflow, Task = Value

    val ordered: Seq[ItemType.Value] = Seq(Project, Workflow, Dataset, Transform, Linking, Task)
  }
  assert(ItemType.values.size == ItemType.ordered.size)

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

  /** A single facet of the search results. */
  case class FacetResult(id: String,
                         label: String,
                         `type`: String,
                         values: FacetValues)

  /** Values of a single facet. */
  sealed trait FacetValues

  /** Values of a keyword based facet. */
  case class KeywordFacetValues(values: Seq[KeywordFacetValue]) extends FacetValues

  /** A single value of a keyword facet. */
  case class KeywordFacetValue(id: String,
                               label: String,
                               count: Option[Int])

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
    // Property names
    final val LABEL = "label"
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

  case class Facet(id: String, label: String)

  object Facets {
    // dataset facets
    final val datasetType: Facet = Facet("datasetType", "Dataset type")

    val facetIds: Seq[String] = Seq(datasetType).map(_.id)
  }

  /** A search request that supports types and facets. */
  case class FacetedSearchRequest(project: Option[String],
                                  itemType: Option[ItemType.Value],
                                  textQuery: Option[String],
                                  offset: Option[Int],
                                  limit: Option[Int],
                                  sortBy: Option[SortBy.Value],
                                  sortOrder: Option[SortOrder.Value],
                                  facets: Option[Seq[FacetSetting]]) extends SearchRequestTrait {
    def workingOffset: Int =  offset.getOrElse(FacetedSearchRequest.DEFAULT_OFFSET)

    def workingLimit: Int = limit.getOrElse(FacetedSearchRequest.DEFAULT_LIMIT)

    // Sort results according to request
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

    /** Collects facet data of dataset tasks. */
    case class DatasetFacetCollector() {
      val datasetTypes = new mutable.ListMap[String, Int]()
      val datasetTypeLabel = new mutable.ListMap[String, String]()

      /** Collect facet values of a single dataset */
      def collect(datasetTask: ProjectTask[DatasetSpec[Dataset]]): Unit = {
        val pluginSpec = datasetTask.plugin.pluginSpec
        val id = pluginSpec.id
        val label = pluginSpec.label
        datasetTypes.put(id, datasetTypes.getOrElseUpdate(id, 0) + 1)
        datasetTypeLabel.put(id, label)
      }

      /** Results of all facets of the dataset type */
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
        FacetResult(Facets.datasetType.id, Facets.datasetType.label, FacetType.keyword.toString, keywordFacetValues)
      }
    }

    /** Collects the result facet values. */
    def resultFacets(tasks: Seq[TypedTasks]): Seq[FacetResult] = {
      val datasetFacetCollector = DatasetFacetCollector()
      for(typedTasks <- tasks) {
        typedTasks.itemType match {
          case ItemType.Dataset =>
            if(itemType.contains(ItemType.Dataset)) {
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
        val project = jsonPropertyStringValue(result, "projectId")
        val itemId = jsonPropertyStringValue(result, "id")
        val links: Seq[ItemLink] = itemTypeReads.reads(result.value("type")).get match {
          case ItemType.Transform => Seq(
            ItemLink("Mapping editor", s"/transform/$project/$itemId/editor"),
            ItemLink("Transform evaluation", s"/transform/$project/$itemId/evaluate"),
            ItemLink("Transform execution", s"/transform/$project/$itemId/execute")
          )
          case ItemType.Linking => Seq(
            ItemLink("Linking editor", s"/linking/$project/$itemId/editor"),
            ItemLink("Linking evaluation", s"/linking/$project/$itemId/evaluate"),
            ItemLink("Linking execution", s"/linking/$project/$itemId/execute")
          )
          case ItemType.Workflow => Seq(
            ItemLink("Workflow editor", s"/workflow/editor/$project/$itemId")
          )
          case _ => Seq.empty
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

    def apply()(implicit userContext: UserContext): JsValue = {
      val ps: Seq[Project] = projects
      var tasks: Seq[TypedTasks] = ps.flatMap(fetchTasks)
      var selectedProjects: Seq[Project] = Seq.empty

      for(term <- textQuery) {
        val lowerCaseTerm = extractSearchTerms(term)
        tasks = tasks.map(typedTasks => filterTasksByTextQuery(typedTasks, lowerCaseTerm))
        selectedProjects = if(itemType.contains(ItemType.Project)) ps.filter(p => matchesSearchTerm(lowerCaseTerm, p)) else Seq()
      }

      // facets are collected after filtering, so only non empty facets are displayed with correct counts
      // FIXME: These are not correctly calculated if multiple facets are applied. Facet values must be calculated with all except this specific facet being applied.
      val facets = resultFacets(tasks)
      tasks = tasks.map(t => filterTasksByFacetSettings(t))
      val jsonResult = selectedProjects.map(toJson) ++ tasks.flatMap(toJson)
      val sorted = sort(jsonResult)
      val resultWindow = sorted.slice(workingOffset, workingOffset + workingLimit)
      val withItemLinks = addItemLinks(resultWindow)

      JsObject(Seq(
        "total" -> JsNumber(BigDecimal(sorted.size)),
        "results" -> JsArray(withItemLinks),
        "facets" -> JsArray(facets.map(fr => Json.toJson(fr)))
      ))
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
    private def matchesFacetSettingFunction(itemType: ItemType.Value): ProjectTask[_ <: TaskSpec] => Boolean = {
      val alwaysTrueFunction: ProjectTask[_ <: TaskSpec] => Boolean = _ => true
      facets match {
        case None => alwaysTrueFunction
        case Some(facetSettings) if facetSettings.isEmpty => alwaysTrueFunction
        case Some(facetSettings) =>
          itemType match { // FIXME: Improve facet filtering code when more facets are added
            case ItemType.Dataset =>
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
          for (it <- Seq(ItemType.Dataset, ItemType.Linking, ItemType.Transform, ItemType.Workflow, ItemType.Task)) {
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
        case ItemType.Transform => project.tasks[TransformSpec]
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
