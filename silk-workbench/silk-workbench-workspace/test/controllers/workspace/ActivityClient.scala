package controllers.workspace

import org.silkframework.util.Identifier
import play.api.libs.json.{JsBoolean, JsString, JsValue}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

  def activityValue(activityId: String, contentType: String = "application/json"): WSResponse = {
    val getActivityValueRequest = client.url(s"$activities/$activityId/value")
    val response = getActivityValueRequest.addHttpHeaders(("ACCEPT", contentType)).get()
    checkResponse(response)
  }

  def activityStatus(activityId: String): JsValue = {
    val getActivityStatusRequest = client.url(s"$activities/$activityId/status")
    val statusResponse = getActivityStatusRequest.get()
    checkResponse(statusResponse).json
  }

  def waitForActivity(activityId: String): Unit = {
    var isRunning = false
    do {
      isRunning = (activityStatus(activityId) \ "isRunning").as[JsBoolean].value
      Thread.sleep(200)
    } while(isRunning)
  }

  private def checkResponse(futureResponse: Future[WSResponse],
                    responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 100.seconds)
    assert(response.status.toString.head == responseCodePrefix, s"Status text: ${response.statusText}. Response Body: ${response.body}")
    response
  }

}
