package controllers.workspaceApi

import java.io.FileOutputStream

import akka.stream.scaladsl.{FileIO, Source}
import controllers.workspaceApi.ProjectImportApi.ProjectImportDetails
import helper.IntegrationTestTrait
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.resource.ClasspathResource
import org.silkframework.util.{Identifier, StreamUtils, TestFileUtils}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json.{JsNull, JsNumber, JsValue, Json}
import play.api.libs.ws.EmptyBody
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

class ProjectImportApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait
    with MustMatchers with PatienceConfiguration {
  behavior of "project import API"

  override def projectPathInClasspath: String = "diProjects/configProject.zip"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  val expectedProjectId = "configProject"

  it should "import a project from a XML file with multi step import API" in {
    val expectedProjectLabel = "Config Project"
    val expectedProjectDescription = "Config project description"

    // Create project with the same ID to test exist-flag
    retrieveOrCreateProject(expectedProjectId)
    val projectCount = workspaceProvider.readProjects().size
    val projectImportId = uploadProjectFile()

    // Fetch project import details
    val detailUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportDetails(projectImportId)
    val detailResponse = Json.fromJson[ProjectImportDetails](checkResponseExactStatusCode(createRequest(detailUrl).get()).json)
    detailResponse.asOpt mustBe defined
    detailResponse.get mustBe ProjectImportDetails("configProject", expectedProjectLabel, Some(expectedProjectDescription),
      XmlZipWithResourcesProjectMarshaling.marshallerId, projectAlreadyExists = true, None)

    // Start project import without conflict resolve strategy -> 409
    val importStartUrl = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId)
    checkResponseExactStatusCode(createRequest(importStartUrl).post(EmptyBody), CONFLICT)

    // Start project import with generateNewId strategy
    val importStartUrlWithNewId = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId, generateNewId = true)
    checkResponseExactStatusCode(createRequest(importStartUrlWithNewId).post(EmptyBody), CREATED)

    // Check import status with a 0 timeout, i.e. it should return immediately and the import should not be finished, yet.
    val importProjectStatusUrlZeroTimeout = controllers.workspaceApi.routes.ProjectImportApi.projectImportExecutionStatus(projectImportId, 0)
    val statusJsonZeroTimeout = checkResponseExactStatusCode(createRequest(importProjectStatusUrlZeroTimeout).get()).json
    val generatedProjectId = (statusJsonZeroTimeout \ "projectId").as[String]
    generatedProjectId must endWith (Identifier.fromAllowed(expectedProjectLabel))
    generatedProjectId must not be expectedProjectId
    (statusJsonZeroTimeout \ "importStarted").toOption mustBe defined
    (statusJsonZeroTimeout \ "importEnded").toOption must not be defined

    // Check import status with the default timeout, i.e. it waits for some amount of time for the project import to finish.
    val importProjectStatusUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportExecutionStatus(projectImportId)
    val statusJson = checkResponseExactStatusCode(createRequest(importProjectStatusUrl).get()).json
    (statusJson \ "importEnded").asOpt[JsNumber] mustBe defined
    (statusJson \ "success").as[Boolean] mustBe true
    (statusJson \ "failureMessage").toOption must not be defined
    workspaceProvider.readProjects().size mustBe (projectCount + 1)
    val newProjectMetaData = workspaceProject(generatedProjectId).config.metaData
    newProjectMetaData.label mustBe expectedProjectLabel
    newProjectMetaData.description mustBe Some(expectedProjectDescription)
  }

  /** Uploads the project archive to the project import endpoint. Return project import ID. */
  private def uploadProjectFile(): String = {
    val uploadUrl = controllers.workspaceApi.routes.ProjectImportApi.uploadProjectArchiveFile()
    val projectResource = ClasspathResource(projectPathInClasspath)
    val responseJson = TestFileUtils.withTempFile { tempFile =>
      StreamUtils.fastStreamCopy(projectResource.inputStream, new FileOutputStream(tempFile), close = true)
      // Upload project file
      val request = createRequest(uploadUrl).post(
        Source(
          FilePart("project", "project.zip", None, FileIO.fromPath(tempFile.toPath)) :: Nil
        )
      )
      checkResponseExactStatusCode(request, CREATED).json
    }
    (responseJson \ ProjectImportApi.PROJECT_IMPORT_ID).as[String]
  }

  it should "overwrite an existing project when overwriteExisting flag is set" in {
    removeProject(expectedProjectId)
    createProject(expectedProjectId)
    workspaceProject(expectedProjectId).allTasks mustBe empty
    val projectImportId = uploadProjectFile()
    // Start project import with  strategy
    val importStartUrlWithNewId = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId, overwriteExisting = true)
    checkResponseExactStatusCode(createRequest(importStartUrlWithNewId).post(EmptyBody), CREATED)
    // wait for import to finish
    val importProjectStatusUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportExecutionStatus(projectImportId)
    val responseJson = checkResponseExactStatusCode(createRequest(importProjectStatusUrl).get()).json
    (responseJson \ "success").as[Boolean] mustBe true
    workspaceProject(expectedProjectId).allTasks must not be empty
  }
}
