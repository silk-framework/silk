package controllers.workspaceApi

import controllers.errorReporting.ErrorReport.ErrorReportItem
import helper.IntegrationTestTrait

import org.silkframework.config.MetaData
import org.silkframework.plugins.dataset.json.JsonParserTask
import org.silkframework.workspace.{SingleProjectWorkspaceProviderTestTrait, WorkspaceFactory}
import play.api.libs.json.Json
import play.api.routing.Router
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ProjectLoadingErrorIntegrationTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with Matchers {
  behavior of "Project failure report API"

  override def projectPathInClasspath: String = "diProjects/projectFailure.zip" // XML project file with manually inserted errors

  override def failOnTaskLoadingErrors: Boolean = false

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private def updateProjectMetaData(): Unit = {
    WorkspaceFactory().workspace.project(projectId).updateMetaData(MetaData(Some(projectLabel)))
  }

  private lazy val tasksReportEndpoint = {
    updateProjectMetaData()
    controllers.projectApi.routes.ProjectApi.projectTasksLoadingErrorReport(projectId).url
  }
  private def taskReportEndpoint(taskId: String): String = {
    updateProjectMetaData()
    controllers.projectApi.routes.ProjectApi.projectTaskLoadingErrorReport(projectId, taskId).url
  }

  private val failingDataset = "testCsv"
  private val failingCustomTask = "rest"
  private val projectLabel = "Project Label"

  it should "report loading failures in Markdown for all tasks" in {
    val markdown = checkResponse(client.url(s"$baseUrl$tasksReportEndpoint").withHttpHeaders(ACCEPT -> TEXT_MARKDOWN).get()).body
    markdown must include ("2 task")
    markdown must include ("## Task 1: test Csv")
    markdown must include (s"## Task 2: $failingCustomTask")
    markdown must include ("* Task ID")
    markdown must include ("* Stacktrace")
    markdown must include ("* Task label: test Csv")
    markdown must include ("* Error message:")
    markdown must include (projectLabel)
  }

  it should "report loading failures in Markdown for a specific task" in {
    val markdown = checkResponse(client.url(s"$baseUrl${taskReportEndpoint(failingDataset)}").withHttpHeaders(ACCEPT -> TEXT_MARKDOWN).get()).body
    markdown must include ("test Csv")
    markdown must include ("* Task ID")
    markdown must include ("* Stacktrace")
    markdown must include ("* Task label: test Csv")
    markdown must include ("* Error message:")
    markdown must include (projectLabel)
  }

  it should "report loading failures in JSON for a specific task" in {
    val jsonReport = checkResponse(client.url(s"$baseUrl${taskReportEndpoint(failingDataset)}").withHttpHeaders(ACCEPT -> APPLICATION_JSON).get()).json
    val errorReport = Json.fromJson[ErrorReportItem](jsonReport).get
    checkFailingDatasetReport(errorReport)
  }

  val otherProject = "otherProject"

  it should "report loading failures in JSON for all tasks" in {
    createProject(otherProject)
    val errorReport = taskErrorReport
    errorReport must have size 2
    errorReport.flatMap(_.taskId) mustBe Seq(failingDataset, failingCustomTask)
    val datasetError = errorReport.head
    checkFailingDatasetReport(datasetError)
  }

  it should "consider changes in the workspace for the loading errors correctly" in {
    // another project should not have errors attached to it
    retrieveOrCreateProject(otherProject).loadingErrors must have size 0
    // When creating a task with the same ID the error should be removed
    project.addTask(failingCustomTask, JsonParserTask())
    taskErrorReport must have size 1
    // When removing the project and creating a new project with the same name, the errors should be gone
    removeProject(projectId)
    createProject(projectId)
    taskErrorReport must have size 0
  }

  private def taskErrorReport: Seq[ErrorReportItem] = {
    val jsonReport = checkResponse(client.url(s"$baseUrl$tasksReportEndpoint").withHttpHeaders(ACCEPT -> APPLICATION_JSON).get()).json
    val errorReport = Json.fromJson[Seq[ErrorReportItem]](jsonReport).get
    errorReport
  }

  private def checkFailingDatasetReport(datasetError: ErrorReportItem): Unit = {
    datasetError.errorMessage must not be empty
    datasetError.errorSummary must not be ""
    datasetError.stackTrace must not be empty
    datasetError.taskLabel mustBe Some("test Csv")
    datasetError.stackTrace.get.lines.size must be > 10
  }
}
