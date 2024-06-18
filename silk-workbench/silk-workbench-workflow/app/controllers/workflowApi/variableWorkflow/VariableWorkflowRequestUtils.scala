package controllers.workflowApi.variableWorkflow

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterObjectValue, ParameterStringValue, ParameterTemplateValue, ParameterValues, PluginContext}
import org.silkframework.runtime.resource.FileMapResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.FileUtils
import org.silkframework.workbench.utils.{NotAcceptableException, UnsupportedMediaTypeException}
import org.silkframework.workbench.workflow.OptionalPrimaryResourceManagerParameter
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.http.MediaRange
import play.api.libs.json._
import play.api.mvc._

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.io.{Codec, Source}

/**
  * Helps to handle replaceable workflow requests.
  */
object VariableWorkflowRequestUtils {
  final val INPUT_FILE_RESOURCE_NAME = "variable_workflow_json_input"
  final val OUTPUT_FILE_RESOURCE_NAME = "variable_workflow_output_file"

  final val QUERY_PARAM_OUTPUT_TYPE = "output:type"

  final val xmlMimeType = "application/xml"
  final val jsonMimeType = "application/json"
  final val ntriplesMimeType = "application/n-triples"
  final val csvMimeTypeShort = "text/csv"
  final val csvMimeType = "text/comma-separated-values"
  final val xlsxMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  final val formUrlEncodedType = "application/x-www-form-urlencoded"

  /* Separator to construct hierarchical query parameters. The minus char is allowed in query parameter names, but disallowed in Scala variable names.
   */
  final val QUERY_PARAM_SEPARATOR = "-"
  // Prefix for query parameters that allow to configure specific features of the variable workflow endpoints.
  final val QUERY_CONFIG_PREFIX = s"config$QUERY_PARAM_SEPARATOR"
  final val QUERY_GENERAL_CONFIG_PREFIX = s"${QUERY_CONFIG_PREFIX}general$QUERY_PARAM_SEPARATOR"
  // Dataset parameter config prefixes
  final val QUERY_DATA_SOURCE_CONFIG_PREFIX = s"${QUERY_CONFIG_PREFIX}dataSourceConfig$QUERY_PARAM_SEPARATOR"
  final val QUERY_DATA_SINK_CONFIG_PREFIX = s"${QUERY_CONFIG_PREFIX}dataSinkConfig$QUERY_PARAM_SEPARATOR"
  // Auto-configure config parameter, either true or false.
  final val QUERY_CONFIG_PARAM_AUTO_CONFIG = s"${QUERY_GENERAL_CONFIG_PREFIX}autoConfig"

  /** The mime types that the variable workflow supports as response. */
  val acceptedMimeType: Seq[String] = Seq(
    jsonMimeType, // JSON dataset
    xmlMimeType, // XML dataset, this is the default for now
    ntriplesMimeType, // RDF file dataset with N-Triples output
    xlsxMimeType, // Excel dataset
    csvMimeTypeShort, // CSV dataset
    csvMimeType // CSV dataset
  )

  /** Returns the output dataset config for the variable workflow based on the ACCEPT header.
    * The second return value is the MIME type that should be returned in the response.
    *
    * @param datasetId The ID of the replaceable dataset in the workflow.
    * */
  private def replaceableDataSinkConfig(datasetId: String,
                                        fileBasedPluginIds: Seq[String])
                                       (implicit request: Request[_], project: Project, userContext: UserContext): VariableDataSinkConfig = {
    request.getQueryString(QUERY_PARAM_OUTPUT_TYPE) match {
      case Some(datasetType) =>
        fromQueryOutputTypeParameter(datasetId, datasetType)
      case None =>
        val pluginId = pluginIdFromAcceptedTypes(request.acceptedTypes, fileBasedPluginIds)
        val (datasetType: String, datasetParameters: Map[String, String], mimeType) = pluginId match {
          case Some("file") => ("file", Map("format" -> "N-Triples"), "application/n-triples")
          case Some("xml") => ("xml", Map.empty, "application/xml")
          case Some("json") => ("json", Map.empty, "application/json")
          case Some("csv") => ("csv", Map.empty, "text/comma-separated-values")
          case Some("excel") => ("excel", Map.empty, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          case Some(pluginId) => (pluginId, Map.empty, s"application/x-plugin-$pluginId")
          case _ =>
            acceptedMimeType.find(mimeType => request.accepts(mimeType)) match {
              case Some(mimeType) =>
                acceptedMimeTypeToSinkConfig(mimeType)
              case None =>
                throw NotAcceptableException("Cannot produce response in any of the requested mime types." +
                  s"Need to set the output type either in the query parameter '$QUERY_PARAM_OUTPUT_TYPE' or by setting the accept header. " +
                  "Supported mime types are: " + acceptedMimeType.mkString(", "))
            }
        }
        val sinkConfig = datasetConfigJson(datasetId, datasetType, datasetParameters ++ datasetParametersFromQuery(QUERY_DATA_SINK_CONFIG_PREFIX), OUTPUT_FILE_RESOURCE_NAME)
        VariableDataSinkConfig(sinkConfig, mimeType)
    }
  }

  private def acceptedMimeTypeToSinkConfig(mimeType: String) = {
    mimeType match {
      case "application/json" => ("json", Map.empty, mimeType)
      case "application/xml" => ("xml", Map.empty, mimeType)
      case "application/n-triples" => ("file", Map("format" -> "N-Triples"), mimeType)
      case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" => ("excel", Map.empty, mimeType)
      case "text/comma-separated-values" | "text/csv" => ("csv", Map.empty, mimeType)
    }
  }

  private def fromQueryOutputTypeParameter(datasetId: String, datasetType: String)
                                          (implicit project: Project, userContext: UserContext): VariableDataSinkConfig = {
    val datasetParams: Map[String, String] =
      if (datasetType == "file") {
        Map("format" -> "N-Triples")
      } else {
        Map.empty
      }
    val sinkConfig = datasetConfigJson(datasetId, datasetType, datasetParams, OUTPUT_FILE_RESOURCE_NAME)
    VariableDataSinkConfig(sinkConfig, jsonMimeType)
  }

  private def pluginIdFromAcceptedTypes(acceptedTypes: Seq[MediaRange],
                                        fileBasedPluginIds: Seq[String]): Option[String] = {
    val r = customMimeTypeRegex
    val pluginId = acceptedTypes
      .map(m => s"${m.mediaType}/${m.mediaSubType}")
      .flatMap (mediaType =>
        r.findFirstMatchIn(mediaType).map(_.group(1))
          .filter(pluginId => fileBasedPluginIds.contains(pluginId))
      )
      .headOption
    pluginId
  }

  case class VariableDataSinkConfig(configJson: JsValue, mimeType: String)

  /** Creates the JSON representation of a dataset config. */
  private def datasetConfigJson(datasetId: String,
                                datasetType: String,
                                datasetParameters: Map[String, String],
                                fileName: String)
                               (implicit project: Project, userContext: UserContext): JsValue = {
    val originalParameters: Map[String, String] = originalDatasetParameters(project, datasetId, datasetType)
    Json.obj(
      "id" -> datasetId,
      "data" -> Json.obj(
        "taskType" -> "Dataset",
        "type" -> datasetType,
        "parameters" -> (originalParameters ++ datasetParameters ++ Map(
          "file" -> fileName
        ))
      )
    )
  }

  def originalDatasetParameters(project: Project,
                                datasetId: String,
                                pluginId: String)
                               (implicit userContext: UserContext): Map[String, String] = {
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    project.taskOption[GenericDatasetSpec](datasetId) match {
      case Some(dataset) =>
        if (dataset.plugin.pluginSpec.id.toString == pluginId) {
          // Copy original parameters of replaceable dataset if the dataset type matches
          dataset.data.parameters.values.collect {
            case (key, value: ParameterStringValue) =>
              (key, value.value)
            case (key, value: ParameterTemplateValue) =>
              (key, value.evaluate(pluginContext.templateVariables.all))
          }
        } else {
          Map.empty
        }
      case None => Map.empty
    }
  }

  private def customMimeTypeRegex = "application/x-plugin-(.*)".r

  private def replaceableDataSourceConfig(datasetId: String,
                                          mediaType: Option[String],
                                          fileBasedPluginIds: Seq[String])
                                         (implicit request: Request[AnyContent], userContext: UserContext, project: Project): JsValue = {
    val multiPartFileContentType = request.body.asMultipartFormData.toSeq.flatMap(_.files.flatMap(_.contentType)).headOption
    val CustomMimeType = customMimeTypeRegex
    val datasetType = multiPartFileContentType.orElse(mediaType) match {
      case Some("application/json") | Some("application/x-www-form-urlencoded") | None =>
        "json"
      case Some("application/xml") =>
        "xml"
      case Some("text/comma-separated-values") | Some("text/csv") =>
        "csv"
      case Some(CustomMimeType(pluginId)) =>
        if(!fileBasedPluginIds.contains(pluginId)) {
          throw UnsupportedMediaTypeException(s"Unsupported custom media type application/x-plugin-$pluginId. No file based plugin with ID $pluginId found.")
        }
        pluginId
      case Some(unsupportedMediaType) =>
        throwUnsupportedMediaType(unsupportedMediaType)
    }
    datasetConfigJson(datasetId, datasetType, datasetParametersFromQuery(), INPUT_FILE_RESOURCE_NAME)
  }

  private def datasetParametersFromQuery(parameterPrefix: String = QUERY_DATA_SOURCE_CONFIG_PREFIX)
                                        (implicit request: Request[_]): Map[String, String] = {
    request.queryString
      .filter(_._1.startsWith(parameterPrefix))
      .map { case (key, values) =>
        val parameterId = key.stripPrefix(parameterPrefix)
        val parameterValue = values.headOption.getOrElse("")
        (parameterId, parameterValue)
      }
  }

  val validMediaTypes = Set(
    jsonMimeType,
    xmlMimeType,
    csvMimeType,
    csvMimeTypeShort,
    "application/x-www-form-urlencoded",
    "multipart/form-data"
  )

  val tempFileBaseDir: Path = {
    val tempDirPath = FileUtils.tempDir
    Paths.get(tempDirPath)
  }

  /**
    * The config for the variable workflow as extracted from the request.
    *
    * @param configParameters       The variable workflow configuration parameters.
    * @param variableDataSinkConfig Optional data sink config.
    */
  case class VariableWorkflowRequestConfig(configParameters: ParameterValues,
                                           variableDataSinkConfig: Option[String])

  private def variableWorkflowFileResourceManager(implicit request: Request[AnyContent]): FileMapResourceManager = {
    val baseDir = Files.createTempDirectory(tempFileBaseDir, "variableWorkflowResourceManager")
    val fileMap = mutable.HashMap[String, Path]()
    for(multipart <- request.body.asMultipartFormData.toSeq;
      (file, idx) <- multipart.files.zipWithIndex) {
      // only get the last part of the filename
      // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
      val filename = Paths.get(file.filename).getFileName
      // FIXME: put content-type when more than 1 uploaded file will be used
//      val contentType = file.contentType
      val managerFile = Paths.get(baseDir.toString, filename.toString)
      file.ref.moveTo(managerFile, replace = true)
      if(idx == 0) {
        fileMap.put(INPUT_FILE_RESOURCE_NAME, managerFile)
      }
      fileMap.put(filename.toString, managerFile)
    }
    FileMapResourceManager(baseDir, fileMap.toMap, removeFilesOnGc = true)
  }

  /** Returns the variable workflow config with the query parameter values as input entity.
    * The data source is always a JSON data source.
    * The data sink is chosen with regards to the ACCEPT header.
    **/
  def requestToWorkflowConfig(workflowTask: Task[Workflow],
                              fileBasedPluginIds: Seq[String])
                             (implicit request: Request[AnyContent], userContext: UserContext, project: Project): VariableWorkflowRequestConfig  = {
    val replaceableDatasets = workflowTask.data.allReplaceableDatasets(project)
    if (replaceableDatasets.sinks.size > 1 || replaceableDatasets.dataSources.size > 1) {
      throw BadUserInputException(s"Workflow task '${workflowTask.label()}' must contain at most one replaceable input " +
        s"and one replaceable output dataset. Instead it has ${replaceableDatasets.dataSources.size} replaceable inputs and ${replaceableDatasets.sinks.size} replaceable outputs.")
    }
    val mediaType = request.contentType map { mediaType =>
      if(customMimeTypeRegex.findFirstIn(mediaType).isEmpty && !validMediaTypes.contains(mediaType)) {
        throwUnsupportedMediaType(mediaType)
      }
      mediaType
    }
    // Optional data source config depending on whether there is a replaceable input dataset or not.
    val dataSourceConfig: Option[JsValue] = replaceableDatasets.dataSources.headOption.map(dataSourceId => replaceableDataSourceConfig(dataSourceId, mediaType, fileBasedPluginIds))
    // Only parse resource if a variable input dataset is defined in the workflow
    val resourceJson: Option[Option[JsValue]] = dataSourceConfig.map(_ => requestToInputResource(mediaType))
    // Optional data sink config and corresponding mime type depending on whether a variable output dataset is part of the workflow.
    val replaceableDataSinkConfigOpt: Option[VariableDataSinkConfig] = replaceableDatasets.sinks.headOption.map(datasetId => replaceableDataSinkConfig(datasetId, fileBasedPluginIds))
    val resourceContentJson = resourceJson match {
      case Some(Some(jsValue)) => Some(jsValue)
      case Some(None) => None
      case None => Some(JsArray(Seq.empty))
    }
    val resources = resourceContentJson match {
      case Some(content) =>
        Json.obj(
          INPUT_FILE_RESOURCE_NAME -> content
        )
      case None =>
        Json.obj()
    }
    val workflowConfig = Json.obj(
      "DataSources" -> dataSourceConfig.toSeq,
      "Sinks" -> replaceableDataSinkConfigOpt.map(_.configJson).toSeq,
      "Resources" -> resources,
      "config" -> Json.obj(
        "autoConfig" -> request.getQueryString(QUERY_CONFIG_PARAM_AUTO_CONFIG).map(_.trim).contains("true")
      )
    )
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    VariableWorkflowRequestConfig(
      configParameters = ParameterValues(Map(
        "configuration" -> ParameterStringValue(workflowConfig.toString()),
        "configurationType" -> ParameterStringValue(jsonMimeType),
        "optionalPrimaryResourceManager" -> ParameterObjectValue(OptionalPrimaryResourceManagerParameter(Some(variableWorkflowFileResourceManager)))
      )),
      variableDataSinkConfig = replaceableDataSinkConfigOpt.map(_.mimeType)
    )
  }

  /* Builds the (JSON) input entity from the request parameters (form URL encoded or query string).
   * Or returns None if the request is a multipart/form-data request and the input file has been uploaded.
   */
  private def requestToInputResource(mediaType: Option[String])
                                    (implicit request: Request[_]): Option[JsValue] = {
    if(request.body.isInstanceOf[AnyContentAsMultipartFormData]) {
      // Input resource is part of multipart/form-data request and will be loaded directly into the resource manager.
      return None
    }
    val inputResource = request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        parametersToJsonResource(v)
      case AnyContentAsJson(jsValue) =>
        jsValue
      case AnyContentAsXml(xml) =>
        JsString(xml.toString())
      case AnyContentAsEmpty if mediaType.isDefined =>
        throw BadUserInputException(s"Content-type (${mediaType.get}) is specified, but request body is empty! " +
            s"If you need to input an 'empty entity', use an empty JSON object or XML element instead as request payload.")
      case AnyContentAsEmpty =>
        // Config parameters will also be included in the input parameters. However in this case, setting config parameters does not (yet) make sense.
        val inputParameters = request.queryString
        if(inputParameters.nonEmpty) {
          parametersToJsonResource(inputParameters)
        } else {
          throw BadUserInputException(s"No input data posted or parameters specified in query string! " +
              s"If you need to input an 'empty entity', use an empty JSON object or XML element instead as request payload.")
        }
      case AnyContentAsRaw(rawBuffer) if mediaType.exists(mt => validMediaTypes.contains(mt)) =>
        val source = Source.fromFile(rawBuffer.asFile)(Codec.UTF8)
        val content =
          try {
            source.mkString
          } finally {
            source.close()
          }
        JsString(content)
      case _ =>
        throwUnsupportedMediaType(request.contentType.getOrElse("-none-"))
    }
    Some(inputResource)
  }

  private def throwUnsupportedMediaType(givenMediaType: String): Nothing = {
    throw UnsupportedMediaTypeException(s"Unsupported payload content type ($givenMediaType). Supported types are: " +
        s"application/json, application/xml, text/csv, text/comma-separated-values and application/x-plugin-<PLUGIN_ID>")
  }

  // Transform a parameter map to a JSON object
  private def parametersToJsonResource(params: Map[String, Seq[String]]): JsValue = {
    val jsonProperties = params.toSeq map { case (paramName, paramValues) =>
      val jsValues = JsArray(paramValues.map(JsString.apply))
      (paramName, jsValues)
    }
    JsObject(jsonProperties)
  }
}
