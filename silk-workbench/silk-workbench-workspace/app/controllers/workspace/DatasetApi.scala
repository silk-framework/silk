package controllers.workspace

import akka.stream.scaladsl.StreamConverters
import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils._
import controllers.util.TextSearchUtils
import controllers.workspace.DatasetApi.TypeCacheFailedException
import controllers.workspace.doc.ResourceApiDoc.ResourceMultiPartRequest
import controllers.workspace.doc.{DatasetApiDoc, ResourceApiDoc}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlResults}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, RequestException}
import org.silkframework.util.Uri
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc._
import resources.ResourceHelper

import java.net.HttpURLConnection
import javax.inject.Inject

@Tag(name = "Datasets", description = "Manage datasets.")
class DatasetApi @Inject() (implicit workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private implicit val partialPath = Json.format[PathCoverage]
  private implicit val valueCoverageMissFormat = Json.format[ValueCoverageMiss]
  private implicit val valueCoverageResultFormat = Json.format[ValueCoverageResult]

  @Operation(
    summary = "Retrieve dataset",
    description = "Retrieve the specification of a dataset.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleJson))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleXml))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  def getDataset(@Parameter(
                  name = "project",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectName: String,
                @Parameter(
                  name = "name",
                  description = "The dataset identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                sourceName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[GenericDatasetSpec](sourceName)
    serializeCompileTime[DatasetTask](task, Some(project))
  }

  @Operation(
    summary = "Auto-configure dataset",
    description = "Retrieve an auto-configured version of the dataset.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleJson))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleXml))
          )
        )
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the dataset type does not support auto-configuration."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  def getDatasetAutoConfigured(@Parameter(
                                 name = "project",
                                 description = "The project identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               projectName: String,
                               @Parameter(
                                 name = "name",
                                 description = "The dataset identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               sourceName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectName)
    implicit val context: PluginContext = PluginContext.fromProject(project)
    val task = project.task[GenericDatasetSpec](sourceName)
    val datasetPlugin = task.data.plugin
    datasetPlugin match {
      case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
        val autoConfDataset = autoConfigurable.autoConfigured
        serializeCompileTime[DatasetTask](PlainTask(task.id, DatasetSpec(autoConfDataset, readOnly = task.readOnly)), Some(project))
      case _ =>
        ErrorResult(BadUserInputException("This dataset type does not support auto-configuration."))
    }
  }

  @Operation(
    summary = "Create or update dataset",
    description = "Create or update a dataset.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "If the dataset has been created or updated."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided dataset specification is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      )
    )
  )
  @RequestBody(
    description = "The dataset specification",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleJson))
      ),
      new Content(
        mediaType = "application/xml",
        examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleXml))
      )
    )
  )
  def putDataset(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 projectName: String,
                 @Parameter(
                   name = "name",
                   description = "The dataset identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 datasetName: String,
                 @Parameter(
                   name = "autoConfigure",
                   description = "If true, the dataset parameters will be auto configured. Only works with dataset plugins that support auto configuration, e.g., CSV.",
                   required = false,
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                 )
                 autoConfigure: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)

    try {
      deserializeCompileTime() { dataset: DatasetTask =>
        if (autoConfigure) {
          dataset.plugin match {
            case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
              project.updateTask(dataset.id, dataset.data.copy(plugin = autoConfigurable.autoConfigured))
              NoContent
            case _ =>
              ErrorResult(BadUserInputException("This dataset type does not support auto-configuration."))
          }
        } else {
          project.updateTask(dataset.id, dataset.data, Some(dataset.metaData))
          NoContent
        }
      }
    } catch {
      case ex: Exception =>
        ErrorResult(BadUserInputException(ex))
    }
  }

  @Operation(
    summary = "Delete dataset",
    description = "Remove a dataset from a project.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "If the dataset has been deleted or there is no dataset with that identifier."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      )
    )
  )
  def deleteDataset(@Parameter(
                      name = "project",
                      description = "The project identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    project: String,
                    @Parameter(
                      name = "name",
                      description = "The dataset identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    source: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.project(project).removeTask[GenericDatasetSpec](source)
    NoContent
  }

  def datasetDialog(projectName: String,
                    datasetName: String,
                    title: String = "Edit Dataset",
                    createDialog: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val datasetPlugin = if (datasetName.isEmpty) None else project.taskOption[GenericDatasetSpec](datasetName).map(_.data)
    Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, datasetPlugin, title, createDialog))
  }

  def datasetDialogAutoConfigured(projectName: String, datasetName: String, pluginId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val createDialog = project.taskOption[DatasetSpec[Dataset]](datasetName).isEmpty
    val dialogTitle = if(createDialog) "Create Dataset" else "Edit Dataset"
    implicit val context: PluginContext = PluginContext.fromProject(project)
    val datasetParams = request.queryString.view.mapValues(_.head).toMap
    val datasetPlugin = Dataset.apply(pluginId, ParameterValues.fromStringMap(datasetParams))
    datasetPlugin match {
      case ds: DatasetPluginAutoConfigurable[_] =>
        Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, Some(DatasetSpec(ds.autoConfigured)), title = dialogTitle, createDialog = createDialog))
      case _ =>
        ErrorResult(BadUserInputException("This dataset type does not support auto-configuration."))
    }
  }

  def dataset(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    context.task.data match {
      case dataset: GenericDatasetSpec =>
        if (dataset.plugin.isInstanceOf[RdfDataset]) {
          Redirect(routes.DatasetController.sparql(project, task))
        } else {
          Redirect(routes.DatasetController.table(project, task))
        }
    }
  }

  def table(project: String, task: String, maxEntities: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    val source = context.task.data.source
    implicit val prefixes: Prefixes = context.project.config.prefixes

    val firstTypes = source.retrieveTypes().head._1
    val paths = source.retrievePaths(firstTypes).toIndexedSeq
    val entityDesc = EntitySchema(firstTypes, paths)
    val entities = source.retrieve(entityDesc).entities.use(_.take(maxEntities).toList)

    Ok(views.html.workspace.dataset.table(context, paths.map(_.toUntypedPath), entities))
  }

  def sparql(project: String, task: String, query: String = ""): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)

    context.task.data.plugin match {
      case rdf: RdfDataset =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        var queryResults: Option[SparqlResults] = None
        if (!query.isEmpty) {
          queryResults = Some(sparqlEndpoint.select(query))
        }
        Ok(views.html.workspace.dataset.sparql(context, sparqlEndpoint, query, queryResults))
      case _ =>
        ErrorResult(BadUserInputException("This is not an RDF-Dataset."))
    }
  }

  /** Get types of a dataset including the search string */
  @deprecated(message = "getDatasetTypes should be used instead.")
  def types(project: String, task: String, search: String = "", limit: Option[Int] = None): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    implicit val prefixes: Prefixes = context.project.config.prefixes

    val typesFull = context.task.activity[TypesCache].value().types
    val typesResolved = typesFull.map(t => new Uri(t).serialize)
    val filteredTypes = typesResolved.filter(_.contains(search))
    val limitedTypes = limit.map(l => filteredTypes.take(l)).getOrElse(filteredTypes)

    Ok(JsArray(limitedTypes.map(JsString)))
  }

  /** Get all types of the dataset */
  @Operation(
    summary = "Dataset types",
    description = "Get a list of entity types of this dataset. Types of a dataset can be classes of an ontology or in the case of a CSV file, a single type.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject("['<http://example.com/Person>', '<http://example.com/Cat>']"))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "If loading types from the dataset failed."
      )
    )
  )
  def getDatasetTypes(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      project: String,
                      @Parameter(
                        name = "name",
                        description = "The dataset identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      task: String,
                      @Parameter(
                        name = "textQuery",
                        description = "An optional multi-word text query to filter the types by.",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String], defaultValue = "")
                      )
                      textQuery: String,
                      @Parameter(
                        name = "limit",
                        description = "Returns max. that many types in the result. If not specified all types are returned.",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[Int])
                      )
                      limit: Option[Int]): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    implicit val prefixes: Prefixes = context.project.config.prefixes
    val typeCache = context.task.activity[TypesCache]
    typeCache.control.waitUntilFinished()

    // Forward any type cache exception
    for(ex <- typeCache.status().exception) {
      throw TypeCacheFailedException(ex)
    }

    // Load and filter types
    val types = typeCache.value().types
    val multiWordQuery = TextSearchUtils.extractSearchTerms(textQuery)
    val filteredTypes = types.filter(typ => TextSearchUtils.matchesSearchTerm(multiWordQuery, typ))
    val limitedTypes = limit.map(l => filteredTypes.take(l)).getOrElse(filteredTypes)
    Ok(JsArray(limitedTypes.map(JsString)))
  }

  @Operation(
    summary = "Retrieve dataset file",
    description = "Retrieve the file of a dataset.",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the dataset is not based on a file."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  def getFile(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "name",
                description = "The dataset identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              datasetName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val dataset = project.task[GenericDatasetSpec](datasetName)
    dataset.data.plugin match {
      case resourceDataset: ResourceBasedDataset =>
        val resource = resourceDataset.file
        Ok.chunked(StreamConverters.fromInputStream(() => resource.inputStream)).withHeaders("Content-Disposition" -> s"""attachment; filename="${resource.name}"""")
      case _ =>
        throw BadUserInputException(s"Dataset ${dataset.labelAndId} is not based on a file.")
    }
  }

  @Operation(
    summary = "Upload dataset file",
    description = ResourceApiDoc.resourceUploadDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "If the resource has been uploaded successfully."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the dataset is not based on a file or has been set to read-only."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "multipart/form-data",
        schema = new Schema(
          implementation = classOf[ResourceMultiPartRequest]
        )
      ),
      new Content(
        mediaType = "application/octet-stream"
      ),
      new Content(
        mediaType = "text/plain"
      ),
    )
  )
  def uploadFile(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "name",
                description = "The dataset identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              datasetName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val dataset = project.task[GenericDatasetSpec](datasetName)
    dataset.data.plugin match {
      case resourceDataset: ResourceBasedDataset =>
        resourceDataset.writableResource match {
          case Some(writeableResource) if !dataset.data.readOnly =>
            ResourceHelper.uploadResource(project, writeableResource)
          case _ =>
            throw BadUserInputException(s"The file of dataset ${dataset.labelAndId} cannot be written to.")
        }
      case _ =>
        throw BadUserInputException(s"Dataset ${dataset.labelAndId} is not based on a file.")
    }
  }

  @Operation(
    summary = "Dataset source path mapping coverage",
    description = DatasetApiDoc.mappingValueCoverageDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.mappingValueCoverageExampleResponse))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "If the dataset type does not support mapping coverage."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[MappingValueCoverageRequest]),
        examples = Array(new ExampleObject("{\"dataSourcePath\": \"/Person/Properties/Property/Value\"}"))
      )
    )
  )
  def getMappingValueCoverage(@Parameter(
                                name = "project",
                                description = "The project identifier",
                                required = true,
                                in = ParameterIn.PATH,
                                schema = new Schema(implementation = classOf[String])
                              )
                              projectName: String,
                              @Parameter(
                                name = "name",
                                description = "The dataset identifier",
                                required = true,
                                in = ParameterIn.PATH,
                                schema = new Schema(implementation = classOf[String])
                              )
                              datasetId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[MappingValueCoverageRequest] { mappingCoverageRequest =>
      val project = WorkspaceFactory().workspace.project(projectName)
      implicit val prefixes: Prefixes = project.config.prefixes
      val datasetTask = project.task[GenericDatasetSpec](datasetId)
      val inputPaths = transformationInputPaths(project)
      val dataSourcePath = UntypedPath.parse(mappingCoverageRequest.dataSourcePath)

      DataSource.pluginSource(datasetTask) match {
        case vd: PathCoverageDataSource with ValueCoverageDataSource =>
          val matchingInputPaths = for (coveragePathInput <- inputPaths;
               inputPath <- coveragePathInput.paths
               if vd.matchPath(coveragePathInput.typeUri, inputPath, dataSourcePath)) yield {
            vd.combinedPath(coveragePathInput.typeUri, inputPath)
          }
          val result = vd.valueCoverage(dataSourcePath, matchingInputPaths)
          Ok(Json.toJson(result))
        case _ =>
          ErrorResult(
            status = INTERNAL_SERVER_ERROR,
            title = "Mapping coverage not supported",
            detail = "The type of data source '" + datasetTask.id.toString + "' does not support mapping value coverage."
          )
      }
    }
  }

  private val FULLY_MAPPED = "fullyMapped"
  private val PARTIALLY_MAPPED = "partiallyMapped"
  private val UNMAPPED = "unmapped"

  private val coverageTypeValues = Seq(FULLY_MAPPED, PARTIALLY_MAPPED, UNMAPPED)

  @Operation(
    summary = "Dataset mapping coverage",
    description = DatasetApiDoc.mappingCoverageDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.mappingCoverageExampleResponse))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "If the dataset type does not support mapping coverage."
      )
    )
  )
  @Parameter(
    name = "type",
    description = "This optional parameter specifies which coverage types should be returned. This is a comma-separated String. Allowed values are 'fullyMapped', 'partiallyMapped' and 'unmapped'. Default is all types.",
    required = false,
    in = ParameterIn.QUERY,
    schema = new Schema(implementation = classOf[String], example = "partiallyMapped,unmapped")
  )
  def getMappingCoverage(@Parameter(
                           name = "project",
                           description = "The project identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         projectName: String,
                         @Parameter(
                           name = "name",
                           description = "The dataset identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         datasetId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val filterPaths = coveragePathFilterFn(request)

    try {
      val project = WorkspaceFactory().workspace.project(projectName)
      implicit val prefixes: Prefixes = project.config.prefixes
      val datasetTask = project.task[GenericDatasetSpec](datasetId)
      DataSource.pluginSource(datasetTask) match {
        case cd: PathCoverageDataSource =>
          getCoverageFromCoverageSource(filterPaths, project, cd)
        case _ =>
          ErrorResult(
            status = INTERNAL_SERVER_ERROR,
            title = "Mapping coverage not supported",
            detail = "The type of data source '" + datasetTask.id.toString + "' does not support mapping value coverage."
          )
      }
    } catch {
      case e: IllegalArgumentException =>
        BadRequest(e.getMessage)
    }
  }

  private def getCoverageFromCoverageSource(filterPaths: PathCoverageResult => Seq[PathCoverage],
                                            project: Project,
                                            cd: PathCoverageDataSource)
                                           (implicit prefixes: Prefixes,
                                            userContext: UserContext): Result = {
    val inputPaths = transformationInputPaths(project)
    val result = cd.pathCoverage(inputPaths.toSeq)
    val filteredPaths = filterPaths(result)
    Ok(Json.toJson(filteredPaths))
  }

  private def transformationInputPaths(project: Project)
                                      (implicit userContext: UserContext): Iterable[CoveragePathInput] = {
    val transformationTasks = project.tasks[TransformSpec]
    for (transformation <- transformationTasks) yield {
      val typeUri = transformation.selection.typeUri
      // TODO: Filter by mapping type, e.g. no URI mapping?
      val paths = transformation.rules.flatMap(_.sourcePaths).distinct
      CoveragePathInput(typeUri.uri, paths.map(_.toUntypedPath))
    }
  }

  /** Filters out paths based on the requested filters. */
  private def coveragePathFilterFn(request: Request[AnyContent]): PathCoverageResult => Seq[PathCoverage] = {
    val (mapped, partialMapped, unmapped) = coverageTypes(request)

    val filteredPaths: PathCoverageResult => Seq[PathCoverage] = result => {
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

object DatasetApi {

  /**
    * Thrown if the type cache failed.
    */
  case class TypeCacheFailedException(cause: Throwable) extends RequestException(cause.getMessage, Option(cause)) {

    def errorTitle: String = "Loading types failed"

    def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_INTERNAL_ERROR)

  }

}

case class MappingValueCoverageRequest(dataSourcePath: String)

object MappingValueCoverageRequest {
  implicit val mappingValueCoverageRequestReads: Format[MappingValueCoverageRequest] = Json.format[MappingValueCoverageRequest]
}