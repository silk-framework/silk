package controllers.workspace

import controllers.errorReporting.ErrorReport.ErrorReportItem
import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.{CustomTask, MetaData}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.activity.{GlobalWorkspaceActivityFactory, ProjectActivityFactory, TaskActivityFactory, WorkspaceActivity}
import org.silkframework.workspace.{Project, ProjectConfig, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.routing.Router

class ActivityApiTest extends PlaySpec with IntegrationTestTrait with BeforeAndAfterAll {

  private val projectId = "project"
  private val taskId = "messageTask"
  private val simpleActivityId = "SimpleActivityFactory"
  private val multiActivityId = "MultiActivityFactory"
  private val failingWorkspaceActivity = "FailingWorkspaceActivityFactory"
  private val failingProjectActivity = "FailingProjectActivityFactory"
  private val failingTaskActivity = "FailingTaskActivityFactory"
  private val message = "Message"

  private val activityClient = new ActivityClient(baseUrl, projectId, taskId)

  override def initWorkspaceBeforeAll: Boolean = false

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[MessageTask])
    PluginRegistry.registerPlugin(classOf[SimpleActivityFactory])
    PluginRegistry.registerPlugin(classOf[MultiActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingActivity])
    PluginRegistry.registerPlugin(classOf[FailingWorkspaceActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingProjectActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingTaskActivityFactory])

    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId, metaData = MetaData.empty))
    project.addTask[MessageTask](taskId, MessageTask(message))
  }

  "start activity in blocking mode" in {
    activityClient.startBlocking(simpleActivityId)
    activityClient.activityValue(simpleActivityId).json mustBe JsString(message)
  }

  "start activity asynchronously" in {
    activityClient.start(simpleActivityId)
    activityClient.waitForActivity(simpleActivityId)
    activityClient.activityValue(simpleActivityId).json mustBe JsString(message)
  }

  "do not allow running multiple singleton activities concurrently" in {
    activityClient.start(simpleActivityId, Map("sleepTime" -> "2000"))
    an[AssertionError] should be thrownBy {
      activityClient.start(simpleActivityId)
    }
  }

  "run multiple non singleton activities" in {
    val activity1 = activityClient.start(multiActivityId, Map("message" -> "1", "sleepTime" -> "2000"))
    val activity2 = activityClient.start(multiActivityId, Map("message" -> "2", "sleepTime" -> "2000"))

    activityClient.waitForActivity(activity1)
    activityClient.waitForActivity(activity2)

    activityClient.activityValue(activity1).json mustBe JsString("1")
    activityClient.activityValue(activity2).json mustBe JsString("2")
  }

  "limit the number of activities that are held in memory" in {
    val createdControlIds =
      for(i <- 0 until WorkspaceActivity.MAX_CONTROLS_PER_ACTIVITY + 1) yield {
        activityClient.start(multiActivityId, Map("message" -> i.toString)).instanceId
      }

    val activityArray = activityClient.activitiesList().as[JsArray].value
    val multiActivity = activityArray.find(activity => (activity \ "name").get == JsString(multiActivityId)).get
    val runningControls = (multiActivity \ "instances").as[JsArray].value
    val runningControlIds = runningControls.map(control => (control \ "id").as[JsString].value)

    runningControlIds mustBe createdControlIds.drop(1)
  }

  "return an error report if an activity has failed" in {
    // Should return 400 if a task is given without project
    ActivityClient.activityErrorReport(baseUrl, failingWorkspaceActivity, taskId=Some("sometask"), expectedCode = BAD_REQUEST)
    // Should 404 if an error report does not exist yet.
    ActivityClient.activityErrorReport(baseUrl, failingWorkspaceActivity, expectedCode = NOT_FOUND)
    // Get error report for workspace level activity
    ActivityClient.startActivityBlocking(baseUrl, failingWorkspaceActivity, expectedStatus = INTERNAL_ERROR)
    val reportWorkspace = JsonHelpers.fromJsonValidated[ErrorReportItem](ActivityClient.activityErrorReport(baseUrl, failingWorkspaceActivity).json)
    reportWorkspace.activityId mustBe Some(failingWorkspaceActivity)
    reportWorkspace.errorMessage mustBe Some("Planned exception")
    reportWorkspace.projectId mustBe empty
    reportWorkspace.taskId mustBe empty
    reportWorkspace.stackTrace must not be empty
    // Get error report for project level activity
    ActivityClient.startActivityBlocking(baseUrl, failingProjectActivity, expectedStatus = INTERNAL_ERROR, projectId = Some(projectId))
    val reportProject = JsonHelpers.fromJsonValidated[ErrorReportItem](ActivityClient.activityErrorReport(baseUrl, failingProjectActivity, projectId = Some(projectId)).json)
    reportProject.projectId mustBe Some(projectId)
    reportProject.taskId mustBe empty
    reportProject.errorMessage mustBe Some("Planned exception")
    // Get error report for task
    ActivityClient.startActivityBlocking(baseUrl, failingTaskActivity, expectedStatus = INTERNAL_ERROR, projectId = Some(projectId), taskId = Some(taskId))
    val reportTask = JsonHelpers.fromJsonValidated[ErrorReportItem](ActivityClient.activityErrorReport(baseUrl,
      failingTaskActivity, projectId = Some(projectId), taskId = Some(taskId)).json)
    reportTask.projectId mustBe Some(projectId)
    reportTask.taskId mustBe Some(taskId)
    reportTask.errorMessage mustBe Some("Planned exception")
    // Get Markdown report
    val markdownReport = ActivityClient.activityErrorReport(baseUrl, failingTaskActivity, projectId = Some(projectId),
      taskId = Some(taskId), accept = "text/markdown").body
    markdownReport must startWith ("# Activity execution error report")
    markdownReport must (include (projectId) and include (taskId) and include (failingTaskActivity) and include ("Planned exception") and include ("ActivityApiTest"))
  }

  override def workspaceProviderId: String = "inMemory"

  protected override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])
}

case class MessageTask(message: String) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}

case class SimpleActivityFactory(sleepTime: Int = 0) extends TaskActivityFactory[MessageTask, Activity[String]] {

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    SimpleActivity(task)
  }

  case class SimpleActivity(task: ProjectTask[MessageTask]) extends Activity[String] {
    override def run(context: ActivityContext[String])
                    (implicit userContext: UserContext): Unit = {
      // Sleep a bit to make sure that multiple activities have to be run at the same time.
      if(sleepTime > 0) {
        Thread.sleep(sleepTime)
      }
      context.value() = task.data.message
    }
  }
}

case class MultiActivityFactory(message: String = "", sleepTime: Int = 0) extends TaskActivityFactory[MessageTask, Activity[String]] {

  override def isSingleton: Boolean = false

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    MultiActivity()
  }

  case class MultiActivity() extends Activity[String] {
    override def run(context: ActivityContext[String])
                    (implicit userContext: UserContext): Unit = {
      // Sleep a bit to make sure that multiple activities have to be run at the same time.
      if(sleepTime > 0) {
        Thread.sleep(sleepTime)
      }
      context.value() = message
    }
  }
}

case class FailingWorkspaceActivityFactory() extends GlobalWorkspaceActivityFactory[FailingActivity] {
  override def apply(): Activity[Unit] = {
    FailingActivity()
  }
}

case class FailingProjectActivityFactory() extends ProjectActivityFactory[FailingActivity] {
  override def apply(project: Project): Activity[Unit] = {
    FailingActivity()
  }
}

case class FailingTaskActivityFactory() extends TaskActivityFactory[MessageTask, FailingActivity] {
  override def apply(task: ProjectTask[MessageTask]): FailingActivity = {
    FailingActivity()
  }
}

class PlannedException(msg: String) extends RuntimeException(msg)

case class FailingActivity() extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    throw new PlannedException("Planned exception")
  }
}
