package controllers.workspaceApi

import akka.stream.scaladsl.{FileIO, Source}
import controllers.workspaceApi.ProjectImportApi.ProjectImportDetails
import helper.IntegrationTestTrait
import org.scalatest.concurrent.PatienceConfiguration

import org.silkframework.runtime.resource.ClasspathResource
import org.silkframework.util.{Identifier, StreamUtils, TestFileUtils}
import org.silkframework.workspace.{SingleProjectWorkspaceProviderTestTrait, WorkspaceFactory}
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json.{JsNumber, Json}
import play.api.libs.ws.EmptyBody
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

import java.io.FileOutputStream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ProjectImportApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait
    with Matchers with PatienceConfiguration {
  behavior of "project import API"

  override def projectPathInClasspath: String = "diProjects/configProject.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  val expectedProjectId = "configProject"

  val workspaceExportZip = "controllers/workspace/workspace.zip"

  it should "import a project from a XML file with multi step import API" in {
    val expectedProjectLabel = "Config Project"
    val expectedProjectDescription = "Config project description"

    // Create project with the same ID to test exist-flag
    retrieveOrCreateProject(expectedProjectId)
    val projectCount = workspaceProvider.readProjects().size
    val projectImportId = uploadProjectFile()

    // Fetch project import details
    requestProjectImportDetails(projectImportId) mustBe ProjectImportDetails("configProject", expectedProjectLabel, Some(expectedProjectDescription),
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
    generatedProjectId must include (Identifier.fromAllowed(expectedProjectLabel))
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
    newProjectMetaData.label mustBe Some(expectedProjectLabel)
    newProjectMetaData.description mustBe Some(expectedProjectDescription)
  }

  private def requestProjectImportDetails(projectImportId: String): ProjectImportDetails = {
    val detailUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportDetails(projectImportId)
    val responseJson = checkResponseExactStatusCode(createRequest(detailUrl).get()).json
    val detailResponse = Json.fromJson[ProjectImportDetails](responseJson).asOpt
    detailResponse mustBe defined
    detailResponse.get
  }

  /** Uploads the project archive to the project import endpoint. Return project import ID. */
  private def uploadProjectFile(projectResourceZip: String = projectPathInClasspath): String = {
    val uploadUrl = controllers.workspaceApi.routes.ProjectImportApi.uploadProjectArchiveFile()
    val projectResource = ClasspathResource(projectResourceZip)
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

  it should "adopt a custom project id" in {
    val customProjectId = "myCustomProjectId"
    WorkspaceFactory().workspace.findProject(customProjectId) mustBe None
    val projectImportId = uploadProjectFile()
    // Fail if generateNewId and newProjectId are both set
    val failingResponse = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId, generateNewId = true, newProjectId = Some(customProjectId))
    checkResponseExactStatusCode(createRequest(failingResponse).post(EmptyBody), BAD_REQUEST)
    // Start project import
    val importStartUrlWithNewId = controllers.workspaceApi.routes.ProjectImportApi.startProjectImport(projectImportId, newProjectId = Some(customProjectId))
    checkResponseExactStatusCode(createRequest(importStartUrlWithNewId).post(EmptyBody), CREATED)
    // wait for import to finish
    val importProjectStatusUrl = controllers.workspaceApi.routes.ProjectImportApi.projectImportExecutionStatus(projectImportId)
    val responseJson = checkResponseExactStatusCode(createRequest(importProjectStatusUrl).get()).json
    (responseJson \ "success").as[Boolean] mustBe true
    workspaceProject(customProjectId).allTasks must not be empty
  }

  it should "return error when a workspace export with more than a single project is uploaded" in {
    val projectImportId = uploadProjectFile(workspaceExportZip)
    val projectImportDetails = requestProjectImportDetails(projectImportId)
    projectImportDetails.errorMessage mustBe defined
    projectImportDetails.errorMessage.get must include ("multiple projects")
  }
}
