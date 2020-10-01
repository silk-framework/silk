package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.{CustomTask, MetaData}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.activity.{TaskActivityFactory, WorkspaceActivity}
import org.silkframework.workspace.{ProjectConfig, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

class ActivityApiTest extends PlaySpec with IntegrationTestTrait {

  private val projectId = "project"
  private val taskId = "messageTask"
  private val simpleActivityId = "SimpleActivityFactory"
  private val multiActivityId = "MultiActivityFactory"
  private val message = "Message"

  private val activityClient = new ActivityClient(baseUrl, projectId, taskId)

  "setup" in {
    PluginRegistry.registerPlugin(classOf[MessageTask])
    PluginRegistry.registerPlugin(classOf[SimpleActivityFactory])
    PluginRegistry.registerPlugin(classOf[MultiActivityFactory])

    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId, metaData = MetaData(projectId)))
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
        activityClient.start(multiActivityId, Map("message" -> i.toString)).toString
      }

    val activityArray = activityClient.activitiesList().as[JsArray].value
    val multiActivity = activityArray.find(activity => (activity \ "name").get == JsString(multiActivityId)).get
    val runningControls = (multiActivity \ "instances").as[JsArray].value
    val runningControlIds = runningControls.map(control => (control \ "id").as[JsString].value)

    runningControlIds mustBe createdControlIds.drop(1)
  }

  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[test.Routes])
}

case class MessageTask(message: String) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}

case class SimpleActivityFactory(sleepTime: Int = 0) extends TaskActivityFactory[MessageTask, Activity[String]] {

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    new Activity[String] {
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

}

case class MultiActivityFactory(message: String = "", sleepTime: Int = 0) extends TaskActivityFactory[MessageTask, Activity[String]] {

  override def isSingleton: Boolean = false

  def apply(task: ProjectTask[MessageTask]): Activity[String] = {
    new Activity[String] {
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

}
