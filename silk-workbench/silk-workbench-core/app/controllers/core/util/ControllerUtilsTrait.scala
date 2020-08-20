package controllers.core.util

import akka.util.ByteString
import controllers.util.SerializationUtils
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.{Project, ProjectTask, Workspace, WorkspaceFactory}
import play.api.http.HttpEntity
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.{BaseController, Request, ResponseHeader, Result}

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

  def projectAndAnyTask(projectId: String, taskId: String)
                       (implicit userContext: UserContext): (Project, ProjectTask[_ <: TaskSpec]) = {
    val project = getProject(projectId)
    (project, project.anyTask(taskId))
  }

  def getProject(projectName: String)(implicit userContext: UserContext): Project = WorkspaceFactory().workspace.project(projectName)

  def task[T <: TaskSpec : ClassTag](projectName: String, taskName: String)
                                    (implicit userContext: UserContext): ProjectTask[T] = {
    val project = getProject(projectName)
    val task = project.task[T](taskName)
    task
  }

  def anyTask(projectId: String, taskId: String)
          (implicit userContext: UserContext): ProjectTask[_ <: TaskSpec] = {
    val project = getProject(projectId)
    project.anyTask(taskId)
  }

  /** Creates a Result object from a byte array. */
  def byteResult(byteArray: Array[Byte], contentType: String, status: Int = OK): Result = {
    Result(
      new ResponseHeader(status, Map.empty),
      HttpEntity.Strict(ByteString(byteArray), Some(contentType))
    )
  }
}
