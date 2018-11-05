package controllers.workflow

import controllers.workflow.WorkflowClient.VariableDatasetPayload
import controllers.workspace.ActivityClient
import org.silkframework.util.Identifier
import play.api.Application
import play.api.http.Writeable
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.libs.ws.{WS, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.xml.{Elem, Null}

class WorkflowClient(baseUrl: String, projectId: Identifier, workflowId: Identifier)(implicit app: Application) {

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
                                 blocking: Boolean = true)(implicit wrt: Writeable[T]): WSResponse = {

    var request: WSRequest = executeOnPayloadUri(projectId, workflowId, blocking)
    if(blocking) {
      request = request.withHeaders("Accept" -> accept)
    }
    val response = request.post(requestBody)
    val result = checkResponse(response)

    if(blocking) {
      result
    } else {
      val activity = new ActivityClient(baseUrl, projectId, workflowId)
      val activityId = (result.json \ "activityId").as[JsString].value
      activity.waitForActivity(activityId)
      activity.activityValue(activityId, accept)
    }
  }

  private def executeOnPayloadUri(projectId: String, workflowId: String, blocking: Boolean) = {
    val urlPostfix = if(blocking) "" else "Asynchronous"
    val request = WS.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId/executeOnPayload$urlPostfix")
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
        "id" -> JsString(datasetId),
        "data" -> JsObject(Seq(
          "taskType" -> JsString("Dataset"),
          "type" -> JsString(datasetPluginType),
          "parameters" -> JsObject(for ((key, value) <- pluginParams ++ additionalParam) yield {
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