package controllers.core.util

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.{JsonFormat, JsonHelpers}
import org.silkframework.workspace.{Project, ProjectTask, Workspace, WorkspaceFactory}
import play.api.libs.json.{JsError, JsString, JsValue, Json, Reads}
import play.api.mvc.{Accepting, BaseController, Request, Result}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
  * Utility methods useful in Controllers.
  */
trait ControllerUtilsTrait {
  this: BaseController =>

  val MARKDOWN_MIME = "text/markdown"
  val AcceptsMarkdown = Accepting(MARKDOWN_MIME)

  /** Validates the JSON of the request body. Returns a 400 with the error details, if the validation failed. */
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

  /** Validates the JSON of the request body. Returns a 400 with the error details, if the validation failed. */
  def validateJsonFromJsonFormat[T](body: T => Result)
                                   (implicit request: Request[JsValue],
                                    jsonFormat: JsonFormat[T],
                                    readContext: ReadContext): Result = {
    val obj: T = try {
      val jsonBody = request.body
      jsonFormat.read(jsonBody)
    } catch {
      case NonFatal(ex) =>
        return BadRequest(Json.obj("status" -> "JSON parse error", "message" -> JsString(ex.getMessage)))
    }
    body(obj)
  }

  /** Returns the workspace object. */
  def workspace(implicit userContext: UserContext): Workspace = {
    WorkspaceFactory().workspace
  }

  /** Returns the typed task and the corresponding project of the task. */
  def projectAndTask[T <: TaskSpec : ClassTag](projectName: String, taskName: String)
                                              (implicit userContext: UserContext): (Project, ProjectTask[T]) = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  /** Returns the untyped task and the corresponding project of the task. */
  def projectAndAnyTask(projectId: String, taskId: String)
                       (implicit userContext: UserContext): (Project, ProjectTask[_ <: TaskSpec]) = {
    val project = getProject(projectId)
    (project, project.anyTask(taskId))
  }

  /** Returns the project with that name of the workspace. */
  def getProject(projectName: String)(implicit userContext: UserContext): Project = workspace.project(projectName)

  /** Returns true if the project exists, false otherwise. */
  def projectExists(projectId: String)(implicit userContext: UserContext): Boolean = {
    workspace.findProject(projectId).isDefined
  }

  def allProjects(implicit userContext: UserContext): Seq[Project] = {
    workspace.projects
  }

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
}
