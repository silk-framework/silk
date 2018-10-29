package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.CustomTask
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.{ProjectConfig, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

class ActivityApiTest extends PlaySpec with IntegrationTestTrait {

  private val projectId = "project"
  private val taskId = "messageTask"
  private val simpleActivityId = "SimpleActivityFactory"
  private val multiActivityId = "MultiActivityFactory"
  private val message = "Message"

  private val client = new ActivityClient(baseUrl, projectId, taskId)

  "setup" in {
    PluginRegistry.registerPlugin(classOf[MessageTask])
    PluginRegistry.registerPlugin(classOf[SimpleActivityFactory])
    PluginRegistry.registerPlugin(classOf[MultiActivityFactory])

    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId))
    project.addTask[MessageTask](taskId, MessageTask(message))
  }

  "start activity in blocking mode" in {
    client.startBlocking(simpleActivityId)
    client.activityValue(simpleActivityId) mustBe JsString(message)
  }

  "start activity asynchronously" in {
    client.start(simpleActivityId)
    client.waitForActivity(simpleActivityId)
    client.activityValue(simpleActivityId) mustBe JsString(message)
  }

  "run multiple non singleton activity" in {
    val activity1 = client.start(multiActivityId, Map("message" -> "1"))
    val activity2 = client.start(multiActivityId, Map("message" -> "2"))

    client.waitForActivity(activity1)
    client.waitForActivity(activity2)

    client.activityValue(activity1) mustBe JsString("1")
    client.activityValue(activity2) mustBe JsString("2")
  }

  override def workspaceProvider: String = "inMemory"

  protected override def routes = Some("test.Routes")
}

case class MessageTask(message: String) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}

case class SimpleActivityFactory() extends TaskActivityFactory[MessageTask, Activity[String]] {

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    new Activity[String] {
      override def run(context: ActivityContext[String])
                      (implicit userContext: UserContext): Unit = {
        context.value() = task.data.message
      }
    }
  }

}

case class MultiActivityFactory(message: String = "") extends TaskActivityFactory[MessageTask, Activity[String]] {

  override def isSingleton: Boolean = false

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    new Activity[String] {
      override def run(context: ActivityContext[String])
                      (implicit userContext: UserContext): Unit = {
        // Sleep a bit to make sure that multiple activities have to be run at the same time.
        Thread.sleep(2000)
        context.value() = message
      }
    }
  }

}
