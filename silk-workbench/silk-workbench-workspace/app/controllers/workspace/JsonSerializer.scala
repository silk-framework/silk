package controllers.workspace

import org.silkframework.config.{LinkSpecification, TransformSpecification}
import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity.Status
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, Task, User}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
  * Generates a JSON describing the current workspace
  */
object JsonSerializer {

  def errorJson(message: String) = {
    Json.obj(
      "error" -> Json.obj(
        "message" -> JsString(message)
      )
    )
  }

  def projectsJson = {
    JsArray (
      for (project <- User().workspace.projects) yield {
        projectJson(project)
      }
    )
  }

  def projectJson(project: Project) = {
    Json.obj(
      "name" -> JsString(project.name),
      "tasks" -> Json.obj(
          "dataset" -> tasksJson[Dataset](project),
          "transform" -> tasksJson[TransformSpecification](project),
          "linking" -> tasksJson[LinkSpecification](project),
          "workflow" -> tasksJson[Workflow](project)
        )
    )
  }

  def tasksJson[T: ClassTag](project: Project) = JsArray(
    for (task <- project.tasks[T]) yield {
      JsString(task.name)
    }
  )

  def projectResources(project: Project) = {
    JsArray(project.resources.list.map(JsString))
  }

  def projectActivities(project: Project) = JsArray(
    for(activity <- project.activities) yield {
      JsString(activity.name)
    }
  )

  def taskActivities(task: Task[_]) = JsArray(
    for(activity <- task.activities) yield {
      JsString(activity.name)
    }
  )

  def activityConfig(config: Map[String, String]) = JsArray(
    for((name, value) <- config.toSeq) yield
      Json.obj("name" -> name, "value" -> value)
  )

  def readActivityConfig(json: JsValue): Map[String, String] = {
    for(value <- json.as[JsArray].value) yield
      ((value \ "name").toString(), (value \ "value").toString)
  }.toMap

  def activityStatus(project: String, task: String, activity: String, status: Status): JsValue = {
    JsObject(
      ("project" -> JsString(project)) ::
      ("task" -> JsString(task)) ::
      ("activity" -> JsString(activity)) ::
      ("statusName" -> JsString(status.name)) ::
      ("isRunning" -> JsBoolean(status.isRunning)) ::
      ("progress" -> JsNumber(status.progress * 100.0)) ::
      ("message" -> JsString(status.toString)) ::
      ("failed" -> JsBoolean(status.failed)) :: Nil
    )
  }
}
