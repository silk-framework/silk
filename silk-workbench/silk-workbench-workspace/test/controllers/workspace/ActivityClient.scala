package controllers.workspace

import org.silkframework.util.Identifier
import play.api.libs.json.{JsBoolean, JsString, JsValue}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import ActivityClient.checkResponse

class ActivityClient(baseUrl: String, projectId: Identifier, taskId: Identifier)(implicit client: WSClient) {

  private val activities = s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities"

  def activitiesList(): JsValue = {
    val response = client.url(activities).get()
    checkResponse(response).json
  }

  def start(activityId: String, parameters: Map[String, String] = Map.empty): Identifier = {
    val startActivityRequest = client.url(s"$activities/$activityId/start")
    val response = startActivityRequest.post(parameters map { case (k, v) => (k, Seq(v)) })
    (checkResponse(response).json \ "activityId").as[JsString].value
  }

  def startBlocking(activityId: String, parameters: Map[String, String] = Map.empty): Unit = {
    val startActivityRequest = client.url(s"$activities/$activityId/startBlocking")
    checkResponse(startActivityRequest.post(parameters map { case (k, v) => (k, Seq(v)) }))
  }

  def activityValue(activityId: String, instanceId: Option[String] = None, contentType: String = "application/json"): WSResponse = {
    val getActivityValueRequest = client.url(s"$activities/$activityId/value" + instanceId.map(id => s"?instance=$id").mkString)
    val response = getActivityValueRequest.addHttpHeaders(("ACCEPT", contentType)).get()
    checkResponse(response)
  }

  def activityStatus(activityId: String, instanceId: Option[String] = None): JsValue = {
    val getActivityStatusRequest = client.url(s"$activities/$activityId/status" + instanceId.map(id => s"?instance=$id").mkString)
    val statusResponse = getActivityStatusRequest.get()
    checkResponse(statusResponse).json
  }

  def waitForActivity(activityId: String, instanceId: Option[String] = None): Unit = {
    var isRunning = false
    do {
      isRunning = (activityStatus(activityId, instanceId) \ "isRunning").as[JsBoolean].value
      Thread.sleep(200)
    } while(isRunning)
  }


}

object ActivityClient {
  def startActivityBlocking(baseUrl: String, activity: String, projectId: Option[String] = None, taskId: Option[String] = None, expectedStatus: Int = 200)(implicit client: WSClient): Unit = {
    val startActivityRequest = client.url(
      s"$baseUrl/workspace/activities/startBlocking${queryString(activity, projectId, taskId)}")
    val response = startActivityRequest.post("")
    checkResponse(response, expectedStatus)
  }

  def activityErrorReport(baseUrl: String,
                          activity: String,
                          projectId: Option[String] = None,
                          taskId: Option[String] = None,
                          expectedCode: Int = 200,
                          accept: String = "application/json")(implicit client: WSClient): WSResponse = {
    val errorReportRequest = client.url(
      s"$baseUrl/workspace/activities/errorReport${queryString(activity, projectId, taskId)}").addHttpHeaders("accept" -> accept)
    val response = errorReportRequest.get()
    checkResponse(response, expectedCode)
  }

  private def queryString(activity: String, projectId: Option[String], taskId: Option[String]) = s"?activity=$activity${projectId.map(id => s"&project=$id").getOrElse("")}${taskId.map(id => s"&task=$id").getOrElse("")}"

  def checkResponse(futureResponse: Future[WSResponse],
                    expectedStatus: Int = 200): WSResponse = {
    val response = Await.result(futureResponse, 100.seconds)
    assert(response.status == expectedStatus, s"Expected status $expectedStatus, but got ${response.status}. Status text: ${response.statusText}. Response Body: ${response.body}")
    response
  }
}
