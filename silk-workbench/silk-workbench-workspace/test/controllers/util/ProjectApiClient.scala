package controllers.util

import controllers.projectApi.ProjectApi.{CreateTagsRequest, ProjectTagsResponse}
import controllers.projectApi.routes.ProjectApi
import helper.ApiClient
import org.silkframework.serialization.json.MetaDataSerializers.{FullTag, MetaDataExpanded, MetaDataPlain}
import org.silkframework.util.Identifier

trait ProjectApiClient extends ApiClient {

  def updateMetaData(projectId: Identifier, metaData: MetaDataPlain): MetaDataPlain = {
    putRequest[MetaDataPlain, MetaDataPlain](ProjectApi.updateProjectMetaData(projectId), metaData)
  }

  def getMetaData(projectId: Identifier): MetaDataPlain = {
    getRequest[MetaDataPlain](ProjectApi.getProjectMetaData(projectId))
  }

  def getMetaDataExpanded(projectId: Identifier): MetaDataExpanded = {
    getRequest[MetaDataExpanded](ProjectApi.getProjectMetaDataExpanded(projectId))
  }

  def retrieveTags(projectId: Identifier, filter: Option[String] = None): ProjectTagsResponse = {
    getRequest[ProjectTagsResponse](ProjectApi.fetchTags(projectId, filter))
  }

  def createTags(projectId: Identifier, addTagRequest: CreateTagsRequest): Iterable[FullTag] = {
    postRequest[CreateTagsRequest, Iterable[FullTag]](ProjectApi.createTags(projectId), addTagRequest)
  }

  def deleteTag(projectId: Identifier, tagUri: String): Unit = {
    deleteRequest(ProjectApi.deleteTag(projectId, tagUri))
  }
}
