package controllers.workspace

import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils._
import org.silkframework.config.Prefixes
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlResults}
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.{Project, User}
import play.api.libs.json._
import play.api.mvc._
import plugins.Context

class DatasetApi extends Controller with ControllerUtilsTrait {
  private implicit val partialPath = Json.format[PathCoverage]
  private implicit val valueCoverageMissFormat = Json.format[ValueCoverageMiss]
  private implicit val valueCoverageResultFormat = Json.format[ValueCoverageResult]

  def getDataset(projectName: String, sourceName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[Dataset](sourceName)
    serializeCompileTime(new DatasetTask(task.id, task.data))
  }

  def getDatasetAutoConfigured(projectName: String, sourceName: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[Dataset](sourceName)
    val datasetPlugin = task.data
    datasetPlugin match {
      case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
        val autoConfDataset = autoConfigurable.autoConfigured
        serializeCompileTime(new DatasetTask(task.id, autoConfDataset))
      case _ =>
        NotImplemented(JsonError("The dataset type does not support auto-configuration."))
    }
  }

  def putDataset(projectName: String, sourceName: String, autoConfigure: Boolean): Action[AnyContent] = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)

    try {
      deserializeCompileTime() { dataset: DatasetTask =>
        if (autoConfigure) {
          dataset.plugin match {
            case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
              project.updateTask(dataset.id, autoConfigurable.autoConfigured.asInstanceOf[Dataset])
              Ok
            case _ =>
              NotImplemented(JsonError("The dataset type does not support auto-configuration."))
          }
        } else {
          project.updateTask(dataset.id, dataset.data)
          Ok
        }
      }
    } catch {
      case ex: Exception => BadRequest(JsonError(ex))
    }
  }
  }

  def deleteDataset(project: String, source: String): Action[AnyContent] = Action {
    User().workspace.project(project).removeTask[Dataset](source)
    Ok
  }

  def datasetDialog(projectName: String, datasetName: String, title: String = "Edit Dataset"): Action[AnyContent] = Action { request =>
    val project = User().workspace.project(projectName)
    val datasetPlugin = if (datasetName.isEmpty) None else project.taskOption[Dataset](datasetName).map(_.data)
    Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, datasetPlugin, title))
  }

  def datasetDialogAutoConfigured(projectName: String, datasetName: String, pluginId: String): Action[AnyContent] = Action { request =>
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    val datasetParams = request.queryString.mapValues(_.head)
    val datasetPlugin = Dataset.apply(pluginId, datasetParams)
    datasetPlugin match {
      case ds: DatasetPluginAutoConfigurable[_] =>
        Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, Some(ds.autoConfigured)))
      case _ =>
        NotImplemented("This dataset plugin does not support auto-configuration.")
    }
  }

  def dataset(project: String, task: String): Action[AnyContent] = Action { implicit request =>
    val context = Context.get[Dataset](project, task, request.path)
    Ok(views.html.workspace.dataset.dataset(context))
  }

  def table(project: String, task: String, maxEntities: Int): Action[AnyContent] = Action { implicit request =>
    val context = Context.get[Dataset](project, task, request.path)
    val source = context.task.data.source

    val firstTypes = source.retrieveTypes().head._1
    val paths = source.retrievePaths(firstTypes).toIndexedSeq
    val entityDesc = EntitySchema(firstTypes, paths.map(_.asStringTypedPath))
    val entities = source.retrieve(entityDesc).take(maxEntities).toList

    Ok(views.html.workspace.dataset.table(context, paths, entities))
  }

  def sparql(project: String, task: String, query: String = ""): Action[AnyContent] = Action { implicit request =>
    val context = Context.get[Dataset](project, task, request.path)

    context.task.data match {
      case rdf: RdfDataset =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        var queryResults: Option[SparqlResults] = None
        if (!query.isEmpty) {
          queryResults = Some(sparqlEndpoint.select(query))
        }
        Ok(views.html.workspace.dataset.sparql(context, sparqlEndpoint, query, queryResults))
      case _ => BadRequest("This is not an RDF-Dataset.")
    }
  }

  /** Get types of a dataset including the search string */
  def types(project: String, task: String, search: String = ""): Action[AnyContent] = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val prefixes = context.project.config.prefixes

    val typesFull = context.task.activity[TypesCache].value.types
    val typesResolved = typesFull.map(prefixes.shorten)
    val allTypes = (typesResolved ++ typesFull).distinct
    val filteredTypes = allTypes.filter(_.contains(search))

    Ok(JsArray(filteredTypes.map(JsString)))
  }

  /** Get all types of the dataset */
  def getDatasetTypes(project: String, task: String): Action[AnyContent] = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val types = context.task.activity[TypesCache].value.types

    Ok(JsArray(types.map(JsString)))
  }

  def getMappingValueCoverage(projectName: String, datasetId: String): Action[JsValue] = Action(BodyParsers.parse.json) { implicit request =>
    validateJson[MappingValueCoverageRequest] { mappingCoverageRequest =>
      val project = User().workspace.project(projectName)
      val datasetTask = project.task[Dataset](datasetId)
      val inputPaths = transformationInputPaths(project)
      val dataSourcePath = Path.parse(mappingCoverageRequest.dataSourcePath)
      datasetTask.source match {
        case vd: PathCoverageDataSource with ValueCoverageDataSource =>
          val matchingInputPaths = for (coveragePathInput <- inputPaths;
               inputPath <- coveragePathInput.paths
               if vd.matchPath(coveragePathInput.typeUri, inputPath, dataSourcePath)) yield {
            vd.combinedPath(coveragePathInput.typeUri, inputPath)
          }
          val result = vd.valueCoverage(dataSourcePath, matchingInputPaths)
          Ok(Json.toJson(result))
        case _ =>
          InternalServerError("The type of data source '" + datasetTask.id.toString + "' does not support mapping value coverage.")
      }
    }
  }

  private val FULLY_MAPPED = "fullyMapped"
  private val PARTIALLY_MAPPED = "partiallyMapped"
  private val UNMAPPED = "unmapped"

  private val coverageTypeValues = Seq(FULLY_MAPPED, PARTIALLY_MAPPED, UNMAPPED)

  def getMappingCoverage(projectName: String, datasetId: String): Action[AnyContent] = Action { request =>
    val filterPaths = coveragePathFilterFn(request)

    try {
      val project = User().workspace.project(projectName)
      implicit val prefixes = project.config.prefixes
      val datasetTask = project.task[Dataset](datasetId)
      datasetTask.source match {
        case cd: PathCoverageDataSource =>
          getCoverageFromCoverageSource(filterPaths, project, cd)
        case _ =>
          InternalServerError("The type of data source '" + datasetTask.id.toString + "' does not support mapping coverage.")
      }
    } catch {
      case e: IllegalArgumentException =>
        BadRequest(e.getMessage)
    }
  }

  private def getCoverageFromCoverageSource(filterPaths: (PathCoverageResult) => Seq[PathCoverage],
                                            project: Project,
                                            cd: PathCoverageDataSource)
                                           (implicit prefixes: Prefixes) = {
    val inputPaths = transformationInputPaths(project)
    val result = cd.pathCoverage(inputPaths.toSeq)
    val filteredPaths = filterPaths(result)
    Ok(Json.toJson(filteredPaths))
  }

  private def transformationInputPaths(project: Project): Traversable[CoveragePathInput] = {
    val transformationTasks = project.tasks[TransformSpec]
    for (transformation <- transformationTasks) yield {
      val typeUri = transformation.selection.typeUri
      // TODO: Filter by mapping type, e.g. no URI mapping?
      val paths = transformation.rules.flatMap(_.paths).distinct
      CoveragePathInput(typeUri.uri, paths)
    }
  }

  /** Filters out paths based on the requested filters. */
  private def coveragePathFilterFn(request: Request[AnyContent]): PathCoverageResult => Seq[PathCoverage] = {
    val (mapped, partialMapped, unmapped) = coverageTypes(request)

    val filteredPaths: (PathCoverageResult) => Seq[PathCoverage] = (result) => {
      result.paths.filter(p =>
        p.covered && p.fully && mapped // fully mapped
            ||
            !p.covered && unmapped // unmapped
            ||
            p.covered && partialMapped // partial mapped
      )
    }
    filteredPaths
  }

  private def coverageTypes(request: Request[AnyContent]) = {
    request.getQueryString("type") match {
      case Some(typeString) =>
        val types = typeString.split(",")
        if (types.exists(!coverageTypeValues.contains(_))) {
          throw new IllegalArgumentException("Invalid coverage types. Allowed types: " + coverageTypeValues.mkString(", "))
        } else {
          (types.contains(FULLY_MAPPED), types.contains(PARTIALLY_MAPPED), types.contains(UNMAPPED))
        }
      case None =>
        (true, true, true)
    }
  }
}

case class MappingValueCoverageRequest(dataSourcePath: String)

object MappingValueCoverageRequest {
  implicit val mappingValueCoverageRequestReads: Format[MappingValueCoverageRequest] = Json.format[MappingValueCoverageRequest]
}