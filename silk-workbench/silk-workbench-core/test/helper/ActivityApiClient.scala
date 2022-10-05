package helper

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.activity.Status.Idle
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ActivitySerializers.StatusJsonFormat
import play.api.libs.json.{JsObject, JsValue, Json}
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.shaded.ahc.org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}

import java.util.logging.Level

trait ActivityApiClient extends ApiClient {

  private val sleepTime = 500

  def runTaskActivity(projectId: String, taskId: String, activityId: String): Unit = {
    val executionId = startTaskActivity(projectId, taskId, activityId)

    var status: Status = Idle()
    do {
      status = taskActivityStatus(projectId, taskId, executionId)
      Thread.sleep(sleepTime)
    } while(status.isRunning)

    for(exception <- status.exception) {
      throw exception
    }
  }

  private def startTaskActivity(projectId: String, taskId: String, activityId: String): String = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities/$activityId/start")
    val response = request.post("")
    (checkResponse(response).body[JsValue].as[JsObject] \ "instanceId").as[String]
  }

  def taskActivityStatus(projectId: String, taskId: String, activityId: String): Status = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities/$activityId/status")
    val response = request.get()
    implicit val readFormat = ReadContext()
    StatusJsonFormat.read(checkResponse(response).body[JsValue])
  }

  def activityValueWebsocket(projectId: String, taskId: String, activityId: String)(f: JsValue => Unit): Unit = {
    val requestUrl = s"ws://localhost:$port/workspace/activities/valueUpdatesWebSocket?project=$projectId&task=$taskId&activity=$activityId"
    val asyncHttpClient = client.underlying[AsyncHttpClient]

    val upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder
    val wsHandler = upgradeHandlerBuilder.addWebSocketListener(new WebSocketListener {
      def onOpen(websocket: WebSocket): Unit = {
        // WebSocket connection opened
      }

      def onClose(websocket: WebSocket, code: Int, reason: String): Unit = {
        // WebSocket connection closed
      }

      def onError(t: Throwable): Unit = {
        // WebSocket connection error
        log.log(Level.WARNING, "Websocket failed", t)
      }

      override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int): Unit = {
        f(Json.parse(payload))
      }

    }).build

    asyncHttpClient.prepareGet(requestUrl).execute(wsHandler).get()
  }

}
