package controllers.workspace

import controllers.errorReporting.ErrorReport.ErrorReportItem
import controllers.workspace.FrequentUpdatesActivityFactory.{DEFAULT_INTERVAL, DEFAULT_UPDATES}
import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.silkframework.config._
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginRegistry}
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.activity.{GlobalWorkspaceActivityFactory, ProjectActivityFactory, TaskActivityFactory, WorkspaceActivity}
import org.silkframework.workspace.{Project, ProjectConfig, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.routing.Router

import java.time.Duration

class ActivityApiTest extends PlaySpec with ConfigTestTrait with IntegrationTestTrait with BeforeAndAfterAll with Eventually {

  private val projectId = "ActivityApiTest-project"
  private val taskId = "messageTask"
  private val simpleActivityId = "SimpleActivityFactory"
  private val multiActivityId = "MultiActivityFactory"
  private val failingWorkspaceActivity = "FailingWorkspaceActivityFactory"
  private val failingProjectActivity = "FailingProjectActivityFactory"
  private val failingTaskActivity = "FailingTaskActivityFactory"
  private val frequentUpdatesActivity = "FrequentUpdatesActivityFactory"
  private val message = "Message"

  private val activityClient = new ActivityClient(baseUrl, projectId, taskId)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(30, Seconds)))

  override def initWorkspaceBeforeAll: Boolean = false

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[MessageTask])
    PluginRegistry.registerPlugin(classOf[SimpleActivityFactory])
    PluginRegistry.registerPlugin(classOf[MultiActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingWorkspaceActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingProjectActivityFactory])
    PluginRegistry.registerPlugin(classOf[FailingTaskActivityFactory])
    PluginRegistry.registerPlugin(classOf[FrequentUpdatesActivityFactory])

    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId, metaData = MetaData.empty))
    project.addTask[MessageTask](taskId, MessageTask(message))
  }

  override def afterAll(): Unit = {
    WorkspaceFactory().workspace.removeProject(projectId)

    PluginRegistry.unregisterPlugin(classOf[MessageTask])
    PluginRegistry.unregisterPlugin(classOf[SimpleActivityFactory])
    PluginRegistry.unregisterPlugin(classOf[MultiActivityFactory])
    PluginRegistry.unregisterPlugin(classOf[FailingWorkspaceActivityFactory])
    PluginRegistry.unregisterPlugin(classOf[FailingProjectActivityFactory])
    PluginRegistry.unregisterPlugin(classOf[FailingTaskActivityFactory])
    PluginRegistry.unregisterPlugin(classOf[FrequentUpdatesActivityFactory])

    super.afterAll()
  }

  "number of concurrent activity executions is configurable" in {
    WorkspaceActivity.maxConcurrentExecutionsPerActivity() mustBe 21
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
      for(i <- 0 until WorkspaceActivity.maxConcurrentExecutionsPerActivity() + 1) yield {
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

  "push value updates into a WebSocket" in {
    var count = 0
    var lastValue: Int = 0

    activityClient.start(frequentUpdatesActivity)
    activityValueWebsocket(projectId, taskId, frequentUpdatesActivity) { value =>
      count += 1
      lastValue = value.as[JsString].value.toInt
    }
    activityClient.waitForActivity(frequentUpdatesActivity)

    // Make sure that the final value has been received
    eventually {
      lastValue mustBe FrequentUpdatesActivityFactory.DEFAULT_UPDATES
    }
    // Make sure that throttling worked
    count must equal (11 +- 5)
  }

  override def workspaceProviderId: String = "inMemory"

  protected override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    WorkspaceActivity.MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY -> Some("21")
  )
}

case class MessageTask(message: String) extends CustomTask {
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)
  override def outputPort: Option[Port] = None
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

case class FailingActivity() extends Activity[Unit] with AnyPlugin {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    throw new PlannedException("Planned exception")
  }
}

case class FrequentUpdatesActivityFactory(numberOfUpdates: Int = DEFAULT_UPDATES,
                                          interval: Duration = DEFAULT_INTERVAL) extends TaskActivityFactory[MessageTask, Activity[String]] {

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    FrequentUpdatesActivity(task)
  }

  case class FrequentUpdatesActivity(task: ProjectTask[MessageTask]) extends Activity[String] {

    override def initialValue: Option[String] = Some("0")

    override def run(context: ActivityContext[String])
                    (implicit userContext: UserContext): Unit = {
      for(i <- 1 to numberOfUpdates) {
        Thread.sleep(interval.toMillis)
        context.value() = i.toString
      }
    }
  }
}

object FrequentUpdatesActivityFactory {

  final val DEFAULT_UPDATES = 100

  final val DEFAULT_INTERVAL = Duration.ofMillis(100)

}
