package controllers.workflow

import controllers.workflow.WorkflowClient.VariableDatasetPayload
import controllers.workflowApi.workflow.WorkflowInfo
import controllers.workspace.ActivityClient
import controllers.workspace.activityApi.StartActivityResponse
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.serialization.json.JsonSerializers.{DATA, ID, PARAMETERS, TASKTYPE, TASK_TYPE_DATASET, TYPE}
import org.silkframework.util.Identifier
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.test.Helpers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.xml.{Elem, Null}

class WorkflowClient(baseUrl: String, projectId: Identifier, workflowId: Identifier, workflowApiPath: String = "/api/workflow")(implicit client: WSClient) {

  def executeVariableWorkflowXml(datasetPayloads: Seq[VariableDatasetPayload], blocking: Boolean = true): WSResponse = {
    val requestXML = {
      <Workflow>
        <DataSources>
          {datasetPayloads.filterNot(_.isSink).map(_.datasetXml)}
        </DataSources>
        <Sinks>
          {datasetPayloads.filter(_.isSink).map(_.datasetXml)}
        </Sinks>{datasetPayloads.map(_.resourceXml)}
      </Workflow>
    }
    executeVariableWorkflow(requestXML, "application/xml", blocking)
  }

  def executeVariableWorkflowJson(datasetPayloads: Seq[VariableDatasetPayload], blocking: Boolean = true): WSResponse = {
    val requestJSON = JsObject(Seq(
      "DataSources" -> {JsArray(datasetPayloads.filterNot(_.isSink).map(_.datasetJson))},
      "Sinks" -> {JsArray(datasetPayloads.filter(_.isSink).map(_.datasetJson))},
      "Resources" -> JsObject(datasetPayloads.flatMap(_.resourceJson))
    ))
    executeVariableWorkflow(requestJSON, "application/json", blocking)
  }

  def executeVariableWorkflow[T](requestBody: T,
                                 accept: String = "application/xml",
                                 blocking: Boolean = true)(implicit wrt: BodyWritable[T]): WSResponse = {

    var request: WSRequest = executeOnPayloadUri(projectId, workflowId, blocking)
    if(blocking) {
      request = request.addHttpHeaders("Accept" -> accept)
    }
    val response = request.post(requestBody)
    val result = checkResponse(response)

    if(blocking) {
      result
    } else {
      val activity = new ActivityClient(baseUrl, projectId, workflowId)
      val response = Json.fromJson[StartActivityResponse](result.json).get
      val location = result.header("Location")
      assert(location.isDefined
          && location.get.startsWith("/workflow/workflows/")
          && location.get.filter(_.isLetter).endsWith("executionExecuteWorkflowWithPayload"),
        "Location header is not set or has wrong value! Value: " + location)
      activity.waitForActivity(response.activityId, Some(response.instanceId))
      activity.activityValue(response.activityId, Some(response.instanceId), contentType = accept)
    }
  }

  /**
    * Returns the result of a simple variable workflow request.
    *
    * @param parameters                The input data of the workflow of the replaceable input dataset.
    * @param usePost                   If a POST request should be used that sends the data in the HTTP body. In a GET request it is send via query parameters.
    * @param acceptMimeType            The MIME type. This specifies the desired return format and output dataset.
    * @param contentOpt                Optional content with MIME type and the content as string.
    * @param additionalQueryParameters Additional query parameters to set.
    */
  def executeSimpleVariableWorkflow(parameters: Map[String, Seq[String]] = Map.empty,
                                    usePost: Boolean = false,
                                    acceptMimeType: String = "application/xml",
                                    contentOpt: Option[(String, String)] = None,
                                    additionalQueryParameters: Map[String, String] = Map.empty): Future[WSResponse] = {
    val post = contentOpt.isDefined || usePost
    val path = {
      if (post) {
        controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultPost(projectId, workflowId).url
      } else {
        controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultGet(projectId, workflowId).url
      }
    }
    var request = client.url(s"$baseUrl$workflowApiPath$path")
      .withHttpHeaders(Helpers.ACCEPT -> acceptMimeType)
    additionalQueryParameters.foreach { queryParam =>
      request = request.addQueryStringParameters(queryParam)
    }
    if (post) {
      contentOpt match {
        case Some(content) =>
          request.withHttpHeaders(Helpers.CONTENT_TYPE -> content._2).post(content._1)
        case None =>
          request.post(parameters)
      }
    } else {
      for ((param, paramValues) <- parameters;
           paramValue <- paramValues) {
        request = request.addQueryStringParameters(param -> paramValue)
      }
      request.get()
    }
  }

  def workflowInfo(): WorkflowInfo = {
    JsonHelpers.fromJsonValidated[WorkflowInfo](checkResponse(client.url(s"$baseUrl/api/workflow/info/$projectId/$workflowId").get()).json)
  }

  private def executeOnPayloadUri(projectId: String, workflowId: String, blocking: Boolean) = {
    val urlPostfix = if(blocking) "" else "Asynchronous"
    val request = client.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId/executeOnPayload$urlPostfix")
    request
  }

  private def checkResponse(futureResponse: Future[WSResponse],
                            responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 100.seconds)
    assert(response.status.toString.head == responseCodePrefix, s"Status text: ${response.statusText}. Response Body: ${response.body}")
    response
  }
}

object WorkflowClient {

  case class VariableDatasetPayload(datasetId: String,
                                    datasetPluginType: String,
                                    pluginParams: Map[String, String],
                                    payLoadOpt: Option[String],
                                    isSink: Boolean) {
    var fileResourceId: String = datasetId + "_file_resource"

    private val additionalParam = if(pluginParams.contains("file")) {
      fileResourceId = pluginParams("file")
      Map()
    } else {
      Map("file" -> fileResourceId)
    }

    lazy val datasetXml: Elem = {
      <Dataset id={datasetId}>
        <DatasetPlugin type={datasetPluginType}>
          {for ((key, value) <- pluginParams ++ additionalParam) yield {
            <Param name={key} value={value}/>
        }}
        </DatasetPlugin>
      </Dataset>
    }

    lazy val datasetJson: JsValue = {
      JsObject(Seq(
        ID -> JsString(datasetId),
        DATA -> JsObject(Seq(
          TASKTYPE -> JsString(TASK_TYPE_DATASET),
          TYPE -> JsString(datasetPluginType),
          PARAMETERS -> JsObject(for ((key, value) <- pluginParams ++ additionalParam) yield {
            key -> JsString(value)
          })
        ))
      ))
    }

    lazy val resourceXml = {
      payLoadOpt match {
        case Some(payload) =>
          <resource name={fileResourceId}>
            {payload}
          </resource>
        case None =>
          Null
      }
    }

    lazy val resourceJson: Option[(String, JsValue)] = {
      payLoadOpt map { payload =>
        fileResourceId -> JsString(payload)
      }
    }
  }
}