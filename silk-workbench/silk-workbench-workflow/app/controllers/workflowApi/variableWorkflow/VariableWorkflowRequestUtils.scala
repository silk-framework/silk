package controllers.workflowApi.variableWorkflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.NotAcceptableException
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request}

/**
  * Helps to handle variable workflow requests.
  */
object VariableWorkflowRequestUtils {
  final val INPUT_FILE_RESOURCE_NAME = "variable_workflow_json_input"
  final val OUTPUT_FILE_RESOURCE_NAME = "variable_workflow_output_file"

  /** The mime types that the variable workflow supports as response. */
  val acceptedMimeType: Seq[String] = Seq(
    "application/xml", // XML dataset, this is the default for now FIXME: switch to JSON with CMEM-3051
    "application/json", // JSON dataset
    "application/n-triples", // RDF file dataset with N-Triples output
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Excel dataset
    "text/comma-separated-values" // CSV dataset
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
          case "text/comma-separated-values" => ("csv", Map.empty)
        }
        val sinkConfig = datasetConfigJson(datasetId, datasetType, datasetParameters, OUTPUT_FILE_RESOURCE_NAME)
        VariableDataSinkConfig(sinkConfig, mimeType)
      case None =>
        throw NotAcceptableException("Could not product response in the accepted mime types. Supported mime types are: "
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

  private def variableDataSourceConfig(datasetId: String)
                                      (implicit request: Request[_]): JsValue = {
    datasetConfigJson(datasetId, "json", Map.empty, INPUT_FILE_RESOURCE_NAME)
  }

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
    // Optional data source config depending on whether there is a variable input dataset or not.
    val dataSourceConfig: Option[JsValue] = variableDatasets.dataSources.headOption.map(variableDataSourceConfig)
    // Only parse resource if a variable input dataset is defined in the workflow
    val resourceJson: Seq[JsValue] = dataSourceConfig.map(_ => queryStringToInputResource).toSeq
    // Optional data sink config and corresponding mime type depending on whether a variable output dataset is part of the workflow.
    val variableDataSinkConfigOpt: Option[VariableDataSinkConfig] = variableDatasets.sinks.headOption.map(variableDataSinkConfig)
    val workflowConfig = Json.obj(
      "DataSources" -> dataSourceConfig.toSeq,
      "Sinks" -> variableDataSinkConfigOpt.map(_.configJson).toSeq,
      "Resources" -> Json.obj(
        INPUT_FILE_RESOURCE_NAME -> JsArray(resourceJson)
      )
    )
    (Map(
      "configuration" -> workflowConfig.toString(),
      "configurationType" -> "application/json"
    ), variableDataSinkConfigOpt.map(_.mimeType))
  }

  // Builds the (JSON) input entity from the request parameters (form URL encoded or query string).
  private def queryStringToInputResource(implicit request: Request[_]): JsValue = {
    val requestParameterValues = request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        v
      case _ =>
        request.queryString
    }

    val jsonProperties = requestParameterValues.toSeq map { case (paramName, paramValues) =>
      val jsValues = JsArray(paramValues.map(JsString.apply))
      (paramName, jsValues)
    }
    JsObject(jsonProperties)
  }
}
