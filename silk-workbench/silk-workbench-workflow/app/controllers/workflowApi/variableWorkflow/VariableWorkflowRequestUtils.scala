package controllers.workflowApi.variableWorkflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.{NotAcceptableException, UnsupportedMediaTypeException}
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._
import play.api.mvc._

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
    **/
  private def replaceableDataSinkConfig(datasetId: String)
                                    (implicit request: Request[_]): VariableDataSinkConfig = {
    request.getQueryString(QUERY_PARAM_OUTPUT_TYPE) match {
      case Some(datasetType) =>
        val datasetParams: Map[String, String] =
          if(datasetType == "file") {
            Map("format" -> "N-Triples")
          } else {
            Map.empty
          }
        val sinkConfig = datasetConfigJson(datasetId, datasetType, datasetParams, OUTPUT_FILE_RESOURCE_NAME)
        VariableDataSinkConfig(sinkConfig, jsonMimeType)
      case None =>
        acceptedMimeType.find(mimeType => request.accepts(mimeType)) match {
          case Some(mimeType) =>
            val (datasetType: String, datasetParameters: Map[String, String]) = mimeType match {
              case "application/json" => ("json", Map.empty)
              case "application/xml" => ("xml", Map.empty)
              case "application/n-triples" => ("file", Map("format" -> "N-Triples"))
              case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" => ("excel", Map.empty)
              case "text/comma-separated-values" | "text/csv" => ("csv", Map.empty)
            }
            val sinkConfig = datasetConfigJson(datasetId, datasetType, datasetParameters, OUTPUT_FILE_RESOURCE_NAME)
            VariableDataSinkConfig(sinkConfig, mimeType)
          case None =>
            throw NotAcceptableException("Cannot produce response in any of the requested mime types." +
              s"Need to set the output type either in the query parameter '$QUERY_PARAM_OUTPUT_TYPE' or by setting the accept header. " +
              "Supported mime types are: " + acceptedMimeType.mkString(", "))
        }
    }
  }

  case class VariableDataSinkConfig(configJson: JsValue, mimeType: String)

  /** Creates the JSON representation of a dataset config. */
  private def datasetConfigJson(datasetId: String,
                                datasetType: String,
                                datasetParameters: Map[String, String],
                                fileName: String): JsValue = {
    Json.obj(
      "id" -> datasetId,
      "data" -> Json.obj(
        "taskType" -> "Dataset",
        "type" -> datasetType,
        "parameters" -> (datasetParameters ++ Map(
          "file" -> fileName
        ))
      )
    )
  }

  private def replaceableDataSourceConfig(datasetId: String,
                                       mediaType: Option[String])
                                      (implicit request: Request[_]): JsValue = {
    val datasetType = mediaType match {
      case Some("application/json") | Some("application/x-www-form-urlencoded") | None =>
        "json"
      case Some("application/xml") =>
        "xml"
      case Some("text/comma-separated-values") | Some("text/csv") =>
        "csv"
      case Some(unsupportedMediaType) =>
        throwUnsupportedMediaType(unsupportedMediaType)
    }
    datasetConfigJson(datasetId, datasetType, Map.empty, INPUT_FILE_RESOURCE_NAME)
  }

  val validMediaTypes = Set(
    jsonMimeType,
    xmlMimeType,
    csvMimeType,
    csvMimeTypeShort,
    "application/x-www-form-urlencoded"
  )

  /** Returns the variable workflow config with the query parameter values as input entity.
    * The data source is always a JSON data source.
    * The data sink is chosen with regards to the ACCEPT header.
    **/
  def queryStringToWorkflowConfig(project: Project,
                                  workflowTask: Task[Workflow])
                                 (implicit request: Request[_], userContext: UserContext): (Map[String, String], Option[String]) = {
    val replaceableDatasets = workflowTask.data.allVariableDatasets(project)
    if(replaceableDatasets.sinks.size > 1 || replaceableDatasets.dataSources.size > 1) {
      throw BadUserInputException(s"Workflow task '${workflowTask.label()}' must contain at most one replaceable input " +
          s"and one replaceable output dataset. Instead it has ${replaceableDatasets.dataSources.size} replaceable inputs and ${replaceableDatasets.sinks.size} replaceable outputs.")
    }
    val mediaType = request.contentType map { mediaType =>
      if(!validMediaTypes.contains(mediaType)) {
        throwUnsupportedMediaType(mediaType)
      }
      mediaType
    }
    // Optional data source config depending on whether there is a replaceable input dataset or not.
    val dataSourceConfig: Option[JsValue] = replaceableDatasets.dataSources.headOption.map(dataSourceId => replaceableDataSourceConfig(dataSourceId, mediaType))
    // Only parse resource if a replaceable input dataset is defined in the workflow
    val resourceJson: Option[JsValue] = dataSourceConfig.map(_ => requestToInputResource(mediaType))
    // Optional data sink config and corresponding mime type depending on whether a replaceable output dataset is part of the workflow.
    val replaceableDataSinkConfigOpt: Option[VariableDataSinkConfig] = replaceableDatasets.sinks.headOption.map(replaceableDataSinkConfig)
    val resourceContentJson = resourceJson match {
      case Some(jsValue) => jsValue
      case None => JsArray(Seq.empty)
    }
    val workflowConfig = Json.obj(
      "DataSources" -> dataSourceConfig.toSeq,
      "Sinks" -> replaceableDataSinkConfigOpt.map(_.configJson).toSeq,
      "Resources" -> Json.obj(
        INPUT_FILE_RESOURCE_NAME -> resourceContentJson
      ),
      "config" -> Json.obj(
        "autoConfig" -> request.getQueryString(QUERY_CONFIG_PARAM_AUTO_CONFIG).map(_.trim).contains("true")
      )
    )
    (Map(
      "configuration" -> workflowConfig.toString(),
      "configurationType" -> jsonMimeType
    ), replaceableDataSinkConfigOpt.map(_.mimeType))
  }

  // Builds the (JSON) input entity from the request parameters (form URL encoded or query string).
  private def requestToInputResource(mediaType: Option[String])(implicit request: Request[_]): JsValue = {
    request.body match {
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
  }

  private def throwUnsupportedMediaType(givenMediaType: String): Nothing = {
    throw UnsupportedMediaTypeException(s"Unsupported payload content type ($givenMediaType). Supported types are: " +
        s"application/json, application/xml, text/csv and text/comma-separated-values")
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
