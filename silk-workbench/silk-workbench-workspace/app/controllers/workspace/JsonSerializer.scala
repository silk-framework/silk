package controllers.workspace

import org.silkframework.config.{LinkSpecification, TransformSpecification}
import org.silkframework.dataset.Dataset
import org.silkframework.workspace.{User, Project}
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._

import scala.reflect.ClassTag

/**
  * Generates a JSON describing the current workspace
  */
object JsonSerializer {

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

  def activityConfig(config: Map[String, String]) = JsArray(
    for((name, value) <- config.toSeq) yield
      Json.obj("name" -> name, "value" -> value)
  )

  def readActivityConfig(json: JsValue): Map[String, String] = {
    for(value <- json.as[JsArray].value) yield
      ((value \ "name").toString(), (value \ "value").toString)
  }.toMap
}
