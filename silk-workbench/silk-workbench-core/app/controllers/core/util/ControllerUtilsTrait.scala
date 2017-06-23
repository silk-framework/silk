package controllers.core.util

import org.silkframework.config.TaskSpec
import org.silkframework.workspace.{Project, ProjectTask, User}
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.{Controller, Request, Result}

import scala.reflect.ClassTag

/**
  * Utility methods useful in Controllers.
  */
trait ControllerUtilsTrait {
  this: Controller =>

  def validateJson[T](body: T => Result)
                     (implicit request: Request[JsValue],
                      rds: Reads[T]): Result = {
    val parsedObject = request.body.validate[T]
    parsedObject.fold(
      errors => {
        BadRequest(Json.obj("status" -> "JSON parse error", "message" -> JsError.toJson(errors)))
      },
      obj => {
        body(obj)
      }
    )
  }

  def projectAndTask[T <: TaskSpec : ClassTag](projectName: String, taskName: String): (Project, ProjectTask[T]) = {
    val project = User().workspace.project(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  def getProject(projectName: String): Project = User().workspace.project(projectName)
}
