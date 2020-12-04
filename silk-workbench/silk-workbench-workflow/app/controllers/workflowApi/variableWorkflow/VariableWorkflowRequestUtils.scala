package controllers.workflowApi.variableWorkflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.{NotAcceptableException, UnsupportedMediaTypeException}
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, AnyContentAsJson, AnyContentAsRaw, AnyContentAsXml, Request}

import scala.io.Source

/**
  * Helps to handle variable workflow requests.
  */
object VariableWorkflowRequestUtils {
  final val INPUT_FILE_RESOURCE_NAME = "variable_workflow_json_input"
  final val OUTPUT_FILE_RESOURCE_NAME = "variable_workflow_output_file"

  final val xmlMimeType = "application/xml"
  final val jsonMimeType = "application/json"
  final val ntriplesMimeType = "application/n-triples"
  final val csvMimeTypeShort = "text/csv"
  final val csvMimeType = "text/comma-separated-values"
  final val xlsxMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  final val formUrlEncodedType = "application/x-www-form-urlencoded"

  /** The mime types that the variable workflow supports as response. */
  val acceptedMimeType: Seq[String] = Seq(
    xmlMimeType, // XML dataset, this is the default for now FIXME: switch to JSON with CMEM-3051
    jsonMimeType, // JSON dataset
    ntriplesMimeType, // RDF file dataset with N-Triples output
    xlsxMimeType, // Excel dataset
    csvMimeTypeShort, // CSV dataset
    csvMimeType // CSV dataset
  )

  /** Returns the output dataset config for the variable workflow based on the ACCEPT header.
    * The second return value is the MIME type that should be returned in the response.
    *
    * @param datasetId The ID of the variable dataset in the workflow.
    **/
  private def variableDataSinkConfig(datasetId: String)
                                    (implicit request: Request[_]): VariableDataSinkConfig = {
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
        throw NotAcceptableException("Cannot produce response in any of the requested mime types. Supported mime types are: "
            + acceptedMimeType.mkString(", "))
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

  private def variableDataSourceConfig(datasetId: String,
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
    val variableDatasets = workflowTask.data.variableDatasets(project)
    if(variableDatasets.sinks.size > 1 || variableDatasets.dataSources.size > 1) {
      throw BadUserInputException(s"Workflow task '${workflowTask.taskLabel()}' must contain at most one variable data source " +
          s"and one variable output dataset. Instead it has ${variableDatasets.dataSources.size} variable sources and ${variableDatasets.sinks.size} variable sinks.")
    }
    val mediaType = request.mediaType map { mt =>
      val mediaType = mt.mediaType + "/" + mt.mediaSubType
      if(!validMediaTypes.contains(mediaType)) {
        throwUnsupportedMediaType(mediaType)
      }
      mediaType
    }
    // Optional data source config depending on whether there is a variable input dataset or not.
    val dataSourceConfig: Option[JsValue] = variableDatasets.dataSources.headOption.map(dataSourceId => variableDataSourceConfig(dataSourceId, mediaType))
    // Only parse resource if a variable input dataset is defined in the workflow
    val resourceJson: Option[JsValue] = dataSourceConfig.map(_ => requestToInputResource(mediaType))
    // Optional data sink config and corresponding mime type depending on whether a variable output dataset is part of the workflow.
    val variableDataSinkConfigOpt: Option[VariableDataSinkConfig] = variableDatasets.sinks.headOption.map(variableDataSinkConfig)
    val resourceContentJson = resourceJson match {
      case Some(jsValue) => jsValue
      case None => JsArray(Seq.empty)
    }
    val workflowConfig = Json.obj(
      "DataSources" -> dataSourceConfig.toSeq,
      "Sinks" -> variableDataSinkConfigOpt.map(_.configJson).toSeq,
      "Resources" -> Json.obj(
        INPUT_FILE_RESOURCE_NAME -> resourceContentJson
      )
    )
    (Map(
      "configuration" -> workflowConfig.toString(),
      "configurationType" -> jsonMimeType
    ), variableDataSinkConfigOpt.map(_.mimeType))
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
        if(request.queryString.nonEmpty) {
          parametersToJsonResource(request.queryString)
        } else {
          throw BadUserInputException(s"No input data posted or parameters specified in query string! " +
              s"If you need to input an 'empty entity', use an empty JSON object or XML element instead as request payload.")
        }
      case AnyContentAsRaw(rawBuffer) if mediaType.exists(mt => validMediaTypes.contains(mt)) =>
        val source = Source.fromFile(rawBuffer.asFile)
        val content = source.mkString
        source.close()
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
