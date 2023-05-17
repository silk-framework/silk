package controllers.util

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.scaladsl.{Flow, Keep, Sink, SinkQueueWithCancel, Source}
import controllers.workspaceApi.ReportUpdates
import helper.ApiClient
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import controllers.workspaceApi.routes.ReportsApi
import org.silkframework.execution.ExecutionReport
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat

import java.util.logging.Logger
import scala.concurrent.Future

trait ReportsApiClient extends ApiClient {

  def currentReport(projectId: String, taskId: String): ExecutionReport = {
    val request = createRequest(ReportsApi.currentReport(projectId, taskId))
    val response = request.get()
    val responseJson = checkResponse(response).body[JsValue]
    implicit val readFormat: ReadContext = TestReadContext()
    ExecutionReportJsonFormat.read(responseJson)
  }

  def currentReportUpdates(projectId: String, taskId: String, timestamp: Long): ReportUpdates = {
    val request = createRequest(ReportsApi.currentReportUpdates(projectId, taskId, timestamp))
    val response = request.get()
    val responseJson = checkResponse(response).body[JsValue]
    Json.fromJson[ReportUpdates](responseJson).get
  }

  def currentReportUpdatesWebsocket(projectId: String, taskId: String)(implicit actorSystem: ActorSystem): SinkQueueWithCancel[ReportUpdates] = {
    import actorSystem.dispatcher
    val url = baseUrl.replaceFirst("http", "ws") + ReportsApi.currentReportUpdatesWebsocket(projectId, taskId).url

    val queueSink = Sink.queue[ReportUpdates]()
    val parseMessageFlow = Flow.fromFunction[Message, ReportUpdates] { msg => Json.fromJson[ReportUpdates](Json.parse(msg.asTextMessage.getStrictText)).get }
    val emptySource = Source.maybe[Message]
    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))

    val (upgradeResponse, queue) =
      emptySource
        .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
        .viaMat(parseMessageFlow)(Keep.left)
        .toMat(queueSink)(Keep.both) // also keep the Future[Done]
        .run()

    val connected = upgradeResponse.map { upgrade =>
      // just like a regular http request we can access response status which is available via upgrade.response.status
      // status code 101 (Switching Protocols) indicates that server support WebSockets
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }
    connected.onComplete(res => log.info(s"Websocket connection to $url: $res."))

    queue
  }

}
