package controllers.workspace

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.workspace.{User, Project}
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

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

}
