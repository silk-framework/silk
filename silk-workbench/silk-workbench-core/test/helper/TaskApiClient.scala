package helper

import org.silkframework.config.MetaData
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WS, WSResponse}

trait TaskApiClient extends ApiClient {

  def getTask(projectId: String, taskId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.get()
    checkResponse(response)
  }

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

  def getMetaData(projectId: String, taskId: String): MetaData = {
    implicit val readContext = ReadContext()
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/metadata")
    val response = request.get()
    val json = checkResponse(response).json
    Serialization.formatForType[MetaData, JsValue].read(json)
  }

  def updateMetaData(projectId: String, taskId: String, metaData: MetaData): Unit = {
    implicit val writeContext = WriteContext[JsValue](projectId = Some(projectId))
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/metadata")
    val response = request.put(Serialization.formatForType[MetaData, JsValue].write(metaData))
    checkResponse(response)
  }

}
