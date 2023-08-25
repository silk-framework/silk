package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.text.TextFileDataset
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.workspace.ProjectConfig
import play.api.libs.json.JsArray
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ResourceApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with BeforeAndAfterAll {
  behavior of "Resource API"

  val projectId = "resourcesProject"
  val nestedResourcePath = "a/nested/path"
  val datasetId = "dataset"
  override def beforeAll(): Unit = {
    super.beforeAll()
    userWorkspace.createProject(ProjectConfig(projectId))
    val project = workspaceProject(projectId)
    val resource = project.resources.getInPath(nestedResourcePath)
    resource.writeString("test")
    project.addTask[GenericDatasetSpec](datasetId, DatasetSpec(TextFileDataset(resource)))
  }

  override def routes: Option[Class[testWorkspace.Routes]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemory"

  private val resourceApi = controllers.workspace.routes.ResourceApi

  it should "download the content of a nested file" in {
    val url = resourceApi.getFile(projectId, nestedResourcePath).url
    checkResponse(client.url(s"$baseUrl$url").get()).body mustBe "test"
  }

  it should "fetch file meta data of a nested file" in {
    val url = resourceApi.getFileMetadata(projectId, nestedResourcePath).url
    val resultJson = checkResponse(client.url(s"$baseUrl$url").get()).json
    (resultJson \ "name").as[String] mustBe "path"
    (resultJson \ "relativePath").as[String] mustBe nestedResourcePath
  }

  it should "show file usages" in {
    val url = resourceApi.fileUsage(projectId, nestedResourcePath).url
    val resultJson = checkResponse(client.url(s"$baseUrl$url").get()).json
    resultJson.as[JsArray].value.map(jsObj => (jsObj \ "id").as[String]) mustBe Seq(datasetId)
  }

  it should "upload new content to a file" in {
    val url = resourceApi.putFile(projectId, nestedResourcePath).url
    checkResponseExactStatusCode(client.url(s"$baseUrl$url").put("new content"), NO_CONTENT)
    workspaceProject(projectId).resources.getInPath(nestedResourcePath, mustExist = true).loadAsString() mustBe "new content"
  }

  it should "delete a nested file" in {
    workspaceProject(projectId).resources.getInPath(nestedResourcePath, mustExist = true)
    val url = resourceApi.deleteFile(projectId, nestedResourcePath).url
    checkResponse(client.url(s"$baseUrl$url").delete())
    intercept[ResourceNotFoundException] {
      workspaceProject(projectId).resources.getInPath(nestedResourcePath, mustExist = true)
    }
  }

  it should "return correct content type for basic files" in {
    val project = workspaceProject(projectId)

    def fileMimeType(name: String, contents: String, expectedMimeType: String): Unit = {
      val resource = project.resources.getInPath(name)
      resource.writeString(contents)
      val url = resourceApi.getFile(projectId, name).url
      val response = checkResponse(client.url(s"$baseUrl$url").get())
      response.contentType mustBe expectedMimeType
    }

    fileMimeType("test.json", "{}", "application/json")
    fileMimeType("test.xml", "{}", "application/xml")
  }
}
