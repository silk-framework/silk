package helper

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.activity.Status.{Finished, Idle}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ActivitySerializers.StatusJsonFormat
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WSResponse

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
    (checkResponse(response).body[JsValue].as[JsObject] \ "activityId").as[String]
  }

  def taskActivityStatus(projectId: String, taskId: String, activityId: String): Status = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities/$activityId/status")
    val response = request.get()
    implicit val readFormat = ReadContext()
    StatusJsonFormat.read(checkResponse(response).body[JsValue])
  }

}
