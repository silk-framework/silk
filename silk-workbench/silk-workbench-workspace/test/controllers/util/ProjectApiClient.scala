package controllers.util

import controllers.projectApi.ProjectApi.{AddTagRequest, AddTagResponse, ProjectTagsResponse}
import helper.ApiClient
import org.silkframework.util.Identifier
import controllers.projectApi.routes.ProjectApi
import org.silkframework.serialization.json.MetaDataSerializers.{MetaDataExpanded, MetaDataPlain}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.EmptyBody

import java.util.logging.Logger

trait ProjectApiClient extends ApiClient {

  private val log = Logger.getLogger(getClass.getName)

  def getMetaData(projectId: Identifier): MetaDataPlain = {
    getRequest[MetaDataPlain](ProjectApi.getProjectMetaData(projectId))
  }

  def getMetaDataExpanded(projectId: Identifier): MetaDataExpanded = {
    getRequest[MetaDataExpanded](ProjectApi.getProjectMetaDataExpanded(projectId))
  }

  def addTagToProject(projectId: Identifier, tagUri: String): Unit = {
    val request = createRequest(ProjectApi.addTagToProject(projectId, tagUri))
    val response = request.post(EmptyBody)
    checkResponse(response)
  }

  def retrieveTags(projectId: Identifier): ProjectTagsResponse = {
    getRequest[ProjectTagsResponse](ProjectApi.fetchTags(projectId))
  }

  def addTag(projectId: Identifier, addTagRequest: AddTagRequest): AddTagResponse = {
    postRequest[AddTagRequest, AddTagResponse](ProjectApi.addTag(projectId), addTagRequest)
  }

  def deleteTag(projectId: Identifier, tagUri: String): Unit = {
    deleteRequest(ProjectApi.deleteTag(projectId, tagUri))
  }

}
