package controllers.workspaceApi

import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.{Json, Reads}
import play.api.routing.Router

import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ActivitiesApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with Matchers
    with BeforeAndAfterAll {
  behavior of "Activities API"

  override def projectPathInClasspath: String = "diProjects/activityapiproject.zip"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  val successWorkflowId = "7fb72ab3-1672-4cdb-8f9e-aeec92667876_successWorkflow"
  val failingWorkflowId = "e7dc14e5-b45b-4dc5-9933-bbc2750630f5_failedWorkflow"

  val otherProject = "otherProject"

  case class TestActivityStatus(project: String, task: String, concreteStatus: String, statusName: String)
  implicit val statusReads: Reads[TestActivityStatus] = Json.reads[TestActivityStatus]

  override def beforeAll(): Unit = {
    super.beforeAll()
    createProject(otherProject)
    createCsvFileDataset(otherProject, "csv", "empty.csv")
  }

  it should "report all activities by default" in {
    val allActivities = fetchActivities()
    allActivities.length mustBe > (5)
    allActivities.filter(_.project == otherProject) must not be empty
    allActivities.filter(_.project == projectId) must not be empty
  }

  it should "report the current activities status" in {
    checkWorkflowStatus(successWorkflowId, ("Not executed", "Idle"))
    Try(executeWorkflow(successWorkflowId))
    Try(executeWorkflow(failingWorkflowId))
    checkWorkflowStatus(successWorkflowId, ("Successful", "Finished"))
    checkWorkflowStatus(failingWorkflowId, ("Failed", "Finished"))
  }

  it should "allow filtering by project and status" in {
    val overallActivities = fetchActivities().length
    val projectActivities = fetchActivities(projectId = Some(projectId)).length
    val otherProjectActivities = fetchActivities(projectId = Some(otherProject)).length
    projectActivities must not be otherProjectActivities
    (projectActivities + otherProjectActivities) mustBe overallActivities
    val successfulActivities = fetchActivities(statusFilter = Some("Successful"))
    val failedActivities = fetchActivities(statusFilter = Some("Failed"))
    val finishedActivities = fetchActivities(statusFilter = Some("Finished"))
    (failedActivities.length + successfulActivities.length) mustBe <= (finishedActivities.length)
    checkActivities(successfulActivities, Seq(successWorkflowId), Seq(failingWorkflowId))
    checkActivities(failedActivities, Seq(failingWorkflowId), Seq(successWorkflowId))
    checkActivities(finishedActivities, Seq(successWorkflowId, failingWorkflowId))
  }

  private def checkActivities(activities: Seq[TestActivityStatus], matchingTaskIds: Seq[String], notMatchingTaskIds: Seq[String] = Seq.empty): Unit = {
    val taskSet = activities.map(_.task).toSet
    taskSet.intersect(matchingTaskIds.toSet) mustBe matchingTaskIds.toSet
    taskSet.intersect(notMatchingTaskIds.toSet).size mustBe 0
  }

  private def checkWorkflowStatus(workflowId: String, status: (String, String)): Unit = {
    fetchActivities().
        filter(_.task == workflowId).
        map(a => (a.concreteStatus, a.statusName)) mustBe Seq(status)
  }

  def fetchActivities(projectId: Option[String] = None, statusFilter: Option[String] = None): Seq[TestActivityStatus] = {
    val queryParameters = Seq(
      projectId.map(id => ("projectId", id)),
      statusFilter.map(status => ("statusFilter", status))
    ).flatten
    val jsonResponse = checkResponse(client.url(s"$baseUrl/api/workspace/taskActivitiesStatus")
        .addHttpHeaders(ACCEPT -> APPLICATION_JSON)
        .withQueryStringParameters(queryParameters: _*)
        .get()).json
    Json.fromJson[Seq[TestActivityStatus]](jsonResponse).get
  }
}
