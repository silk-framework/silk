package controllers.workspaceApi

import java.io.FileOutputStream

import akka.stream.scaladsl.{FileIO, Source}
import controllers.workspaceApi.ProjectImportApi.ProjectImportDetails
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.resource.ClasspathResource
import org.silkframework.util.{Identifier, StreamUtils, TestFileUtils}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json.Json
import play.api.libs.ws.EmptyBody
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

class ProjectImportApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with MustMatchers {
  behavior of "project import API"

  override def projectPathInClasspath: String = "diProjects/configProject.zip"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  it should "import a project from a XML file" in {
    val expectedProjectId = "configProject"
    val expectedProjectLabel = "Config Project"
    val expectedProjectDescription = "Config project description"
    // Create project with the same ID to test exist-flag
    retrieveOrCreateProject(expectedProjectId)
    val projectCount = workspaceProvider.readProjects().size
    val uploadUrl = controllers.workspaceApi.routes.ProjectImportApi.uploadProjectArchiveFile()
    val projectResource = ClasspathResource(projectPathInClasspath)
    val responseJson = TestFileUtils.withTempFile { tempFile =>
      StreamUtils.fastStreamCopy(projectResource.inputStream, new FileOutputStream(tempFile), close = true)
      val request = createRequest(uploadUrl).post(
        Source(
          FilePart("project", "project.zip", None, FileIO.fromPath(tempFile.toPath)) :: Nil
        )
      )
      checkResponse(request).json
    }
    val projectImportId = (responseJson \ ProjectImportApi.PROJECT_IMPORT_ID).as[String]
    val detailUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportDetails(projectImportId)
    val detailResponse = Json.fromJson[ProjectImportDetails](checkResponse(createRequest(detailUrl).get()).json)
    detailResponse.asOpt mustBe defined
    detailResponse.get mustBe ProjectImportDetails("configProject", expectedProjectLabel, Some(expectedProjectDescription),
      XmlZipWithResourcesProjectMarshaling.marshallerId, projectAlreadyExists = true, None)
    val importStartUrl = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId)
    checkResponse(createRequest(importStartUrl).post(EmptyBody), '4')
    val importStartUrlWithNewId = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId, generateNewId = true)
    val projectJson = checkResponse(createRequest(importStartUrlWithNewId).post(EmptyBody)).json
    workspaceProvider.readProjects().size mustBe (projectCount + 1)
    val generatedProjectId = (projectJson \ "name").as[String]
    generatedProjectId must endWith (Identifier.fromAllowed(expectedProjectLabel))
    generatedProjectId must not be expectedProjectId
    (projectJson \ "metaData" \ "label").as[String] mustBe expectedProjectLabel
    (projectJson \ "metaData" \ "description").as[String] mustBe expectedProjectDescription
  }
}
