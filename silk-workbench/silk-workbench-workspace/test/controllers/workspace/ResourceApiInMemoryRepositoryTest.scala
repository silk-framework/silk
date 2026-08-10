package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.ProjectConfig
import org.silkframework.workspace.resources.{InMemoryResourceRepository, ResourceRepository}

import java.io.File

/**
  * Resource API tests that run against an in-memory resource repository, which is one of the supported
  * `workspace.repository.plugin` values. Unlike the file based repositories, its resource manager keys resources by
  * their plain name, so resolving a nested path in one step and segment by segment are not the same location.
  */
class ResourceApiInMemoryRepositoryTest extends AnyFlatSpec with IntegrationTestTrait with Matchers {
  behavior of "Resource API on an in-memory repository"

  override def createResourceRepository(dir: File): ResourceRepository = InMemoryResourceRepository()

  override def routes: Option[Class[testWorkspace.Routes]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val resourceApi = controllers.workspace.routes.ResourceApi

  it should "read back and delete a file that has been uploaded to a nested path" in {
    val projectId = "inMemoryResourcesProject"
    val nestedResourcePath = "a/nested/path"
    userWorkspace.createProject(ProjectConfig(projectId))

    val uploadUrl = resourceApi.putFile(projectId, nestedResourcePath).url
    checkResponseExactStatusCode(client.url(s"$baseUrl$uploadUrl").put("content"), NO_CONTENT)

    val downloadUrl = resourceApi.getFileForDownload(projectId, nestedResourcePath).url
    checkResponse(client.url(s"$baseUrl$downloadUrl").get()).body mustBe "content"

    val deleteUrl = resourceApi.deleteFile(projectId, nestedResourcePath).url
    checkResponse(client.url(s"$baseUrl$deleteUrl").delete())
  }
}
