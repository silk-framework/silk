package controllers.core.util

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.{Project, ProjectTask, Workspace, WorkspaceFactory}
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.{BaseController, Request, Result}

import scala.reflect.ClassTag

/**
  * Utility methods useful in Controllers.
  */
trait ControllerUtilsTrait {
  this: BaseController =>

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

  def workspace(implicit userContext: UserContext): Workspace = {
    WorkspaceFactory().workspace
  }

  def projectAndTask[T <: TaskSpec : ClassTag](projectName: String, taskName: String)
                                              (implicit userContext: UserContext): (Project, ProjectTask[T]) = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  def getProject(projectName: String)(implicit userContext: UserContext): Project = WorkspaceFactory().workspace.project(projectName)

  def task[T <: TaskSpec : ClassTag](projectName: String, taskName: String)
                                    (implicit userContext: UserContext): ProjectTask[T] = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[T](taskName)
    task
  }
}
