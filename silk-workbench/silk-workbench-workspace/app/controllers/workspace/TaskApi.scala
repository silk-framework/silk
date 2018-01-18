package controllers.workspace

import controllers.util.SerializationUtils
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.User
import play.api.libs.json.JsBoolean
import play.api.mvc.{Action, AnyContent, Controller}

class TaskApi  extends Controller {

  def postTask(projectName: String): Action[AnyContent] = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      project.addAnyTask(task.id, task.data, task.metaData)
      Ok
    }
  }}

  def putTask(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      if(task.id.toString != taskName) {
        throw new BadUserInputException(s"Inconsistent task identifiers: Got $taskName in URL, but ${task.id} in payload.")
      }
      project.updateAnyTask(task.id, task.data, task.metaData)
      Ok
    }
  }}

  def getTask(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request => {
    implicit val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)

    SerializationUtils.serializeCompileTime[Task[TaskSpec]](task)
  }}

  def deleteTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def putTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    implicit val readContext = ReadContext()

    SerializationUtils.deserializeCompileTime[MetaData](defaultMimeType = "application/json") { metaData =>
      task.updateMetaData(metaData)
      Ok
    }
  }}

  def getTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    Ok(JsonSerializer.taskMetadata(task))
  }

  def cloneTask(projectName: String, oldTask: String, newTask: String) = Action {
    val project = User().workspace.project(projectName)
    project.addAnyTask(newTask, project.anyTask(oldTask))
    Ok
  }

  def cachesLoaded(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val cachesLoaded = task.activities.filter(_.autoRun).forall(!_.status.isRunning)

    Ok(JsBoolean(cachesLoaded))
  }

}
