package controllers.util

import controllers.projectApi.ProjectApi.{AddTagRequest, ProjectTagsResponse}
import helper.ApiClient
import org.silkframework.util.Identifier
import controllers.projectApi.routes.ProjectApi
import org.silkframework.serialization.json.MetaDataSerializers.{FullTag, MetaDataExpanded, MetaDataPlain}
import play.api.libs.ws.EmptyBody

trait ProjectApiClient extends ApiClient {

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

  def addTag(projectId: Identifier, addTagRequest: AddTagRequest): FullTag = {
    postRequest[AddTagRequest, FullTag](ProjectApi.createTag(projectId), addTagRequest)
  }

  def deleteTag(projectId: Identifier, tagUri: String): Unit = {
    deleteRequest(ProjectApi.deleteTag(projectId, tagUri))
  }

}
