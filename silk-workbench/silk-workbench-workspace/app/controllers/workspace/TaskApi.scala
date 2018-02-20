package controllers.workspace

import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.{Project, User}
import play.api.libs.json.{JsBoolean, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller}

class TaskApi extends Controller with ControllerUtilsTrait {

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

  def patchTask(projectName: String, taskName: String): Action[JsValue] = Action(BodyParsers.parse.json) { implicit request => {
    // Load current task
    val project = User().workspace.project(projectName)
    val currentTask = project.anyTask(taskName)

    // Update task JSON
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)
    val currentJson = toJson[Task[TaskSpec]](currentTask).as[JsObject]
    val updatedJson = currentJson.deepMerge(request.body.as[JsObject])

    // Update task
    implicit val writeContext = WriteContext(prefixes = project.config.prefixes, projectId = None)
    val updatedTask = fromJson[Task[TaskSpec]](updatedJson)
    if(updatedTask.id.toString != taskName) {
      throw new BadUserInputException(s"Inconsistent task identifiers: Got $taskName in URL, but ${updatedTask.id} in payload.")
    }
    project.updateAnyTask(updatedTask.id, updatedTask.data, updatedTask.metaData)

    Ok
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

  def copyTask(projectName: String, taskName: String) = Action(BodyParsers.parse.json) { implicit request =>
    implicit val jsonReader = Json.reads[CopyTaskRequest]
    validateJson[CopyTaskRequest] { copyRequest =>
      copyRequest.copy(projectName, taskName)
      Ok
    }
  }

  def cachesLoaded(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val cachesLoaded = task.activities.filter(_.autoRun).forall(!_.status.isRunning)

    Ok(JsBoolean(cachesLoaded))
  }

  /**
    * Request to copy a task to another project.
    */
  case class CopyTaskRequest(targetProject: String) {

    def copy(sourceProject: String, taskName: String): Unit = {
      val sourceProj = User().workspace.project(sourceProject)
      val targetProj = User().workspace.project(targetProject)

      sourceProj.synchronized {
        targetProj.synchronized {
          // We need to generate unique task identifiers
          val identifiers = new IdentifierGenerator
          targetProj.allTasks.foreach(t => identifiers.add(t.id))
          // Copy all tasks
          for(task <- collectTasks(sourceProj, taskName)) {
            targetProj.addAnyTask(identifiers.generate(task.id), task.data, task.metaData)
            // Copy resources if the resource is not in the target project yet
            for(resource <- task.referencedResources if resource.exists && !targetProj.resources.exists(resource.name)) {
              targetProj.resources.get(resource.name).writeResource(resource)
            }
          }
        }
      }
    }

    /**
      * Returns a task and all its referenced tasks.
      */
    private def collectTasks(project: Project, taskName: Identifier): Seq[Task[_ <:TaskSpec]] = {
      val task = project.anyTask(taskName)
      Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
    }

  }

}
