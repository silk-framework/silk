package helper

import play.api.libs.json.JsValue
import play.api.libs.ws.{WS, WSResponse}

trait TaskApiClient extends ApiClient {

  def putTask(projectId: String, taskId: String, taskJson: JsValue): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.put(taskJson)
    checkResponse(response)
  }

  def patchTask(projectId: String, taskId: String, taskJson: JsValue): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.patch(taskJson)
    checkResponse(response)
  }

}
