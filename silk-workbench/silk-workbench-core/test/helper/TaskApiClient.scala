package helper

import org.silkframework.config.MetaData
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.MetaDataSerializers.MetaDataPlain
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse

trait TaskApiClient extends ApiClient {

  def getTask(projectId: String, taskId: String, accept: String = "application/xml"): WSResponse = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId").withHttpHeaders("Accept" -> accept)
    val response = request.get()
    checkResponse(response)
  }

  def putTask(projectId: String, taskId: String, body: String, mimeType: String): WSResponse = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId").withHttpHeaders("Content-Type" -> mimeType)
    val response = request.put(body)
    checkResponse(response)
  }

  def putTask(projectId: String, taskId: String, taskJson: JsValue): WSResponse = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.put(taskJson)
    checkResponse(response)
  }

  def patchTask(projectId: String, taskId: String, taskJson: JsValue): WSResponse = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.patch(taskJson)
    checkResponse(response)
  }

  def getMetaData(projectId: String, taskId: String): MetaData = {
    implicit val readContext = ReadContext()
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/metadata")
    val response = request.get()
    val json = checkResponse(response).json
    Json.fromJson[MetaDataPlain](json).get.toMetaData
  }

  def updateMetaData(projectId: String, taskId: String, metaData: MetaData): Unit = {
    val request = client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/metadata")
    val response = request.put(Json.toJson(MetaDataPlain.fromMetaData(metaData)))
    checkResponse(response)
  }

}
