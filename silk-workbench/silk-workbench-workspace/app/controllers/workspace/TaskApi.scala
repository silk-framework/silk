package controllers.workspace

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.ResourceBasedDataset
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.{Project, WorkspaceFactory}
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.ErrorResult
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsBoolean, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller}

import scala.concurrent.ExecutionContext.Implicits.global

class TaskApi extends Controller with ControllerUtilsTrait {

  def postTask(projectName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      project.addAnyTask(task.id, task.data, task.metaData)
      Ok
    }
  }

  def putTask(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources, project.config.prefixes)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      if(task.id.toString != taskName) {
        throw new BadUserInputException(s"Inconsistent task identifiers: Got $taskName in URL, but ${task.id} in payload.")
      }
      project.updateAnyTask(task.id, task.data, Some(task.metaData))
      Ok
    }
  }

  def patchTask(projectName: String, taskName: String): Action[JsValue] = RequestUserContextAction(BodyParsers.parse.json) { implicit request => implicit userContext =>
    // Load current task
    val project = WorkspaceFactory().workspace.project(projectName)
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
    project.updateAnyTask(updatedTask.id, updatedTask.data, Some(updatedTask.metaData))

    Ok
  }

  def getTask(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    SerializationUtils.serializeCompileTime[Task[TaskSpec]](task)
  }

  def deleteTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def putTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    implicit val readContext = ReadContext()

    SerializationUtils.deserializeCompileTime[MetaData](defaultMimeType = "application/json") { metaData =>
      task.updateMetaData(metaData)
      Ok
    }
  }

  def getTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    val formatOptions =
      TaskFormatOptions(
        includeMetaData = Some(false),
        includeTaskData = Some(false),
        includeTaskProperties = Some(false),
        includeRelations = Some(true),
        includeSchemata = Some(true)
      )
    val taskFormat = new TaskJsonFormat(formatOptions, Some(userContext))(TaskSpecJsonFormat)
    implicit val writeContext = WriteContext[JsValue](projectId = Some(projectName))
    val taskJson = taskFormat.write(task)
    val metaDataJson = JsonSerializers.toJson(task.metaData)
    val mergedJson = metaDataJson.as[JsObject].deepMerge(taskJson.as[JsObject])

    Ok(mergedJson)
  }

  def cloneTask(projectName: String, oldTask: String, newTask: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.addAnyTask(newTask, project.anyTask(oldTask))
    Ok
  }

  def copyTask(projectName: String,
               taskName: String): Action[JsValue] = RequestUserContextAction(BodyParsers.parse.json) { implicit request => implicit userContext =>
    implicit val jsonReader = Json.reads[CopyTaskRequest]
    implicit val jsonWriter = Json.writes[CopyTaskResponse]
    validateJson[CopyTaskRequest] { copyRequest =>
      val result = copyRequest.copy(projectName, taskName)
      Ok(Json.toJson(result))
    }
  }

  def cachesLoaded(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val cachesLoaded = task.activities.filter(_.autoRun).forall(!_.status.isRunning)

    Ok(JsBoolean(cachesLoaded))
  }

  def downloadOutput(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    task.data.outputTasks.headOption match {
      case Some(outputId) =>
        project.taskOption[GenericDatasetSpec](outputId).map(_.data.plugin) match {
          case Some(ds: ResourceBasedDataset) =>
            Ok.stream(Enumerator.fromStream(ds.file.inputStream)).withHeaders("Content-Disposition" -> s"attachment; filename=${ds.file.name}")
          case Some(_) =>
            ErrorResult(BAD_REQUEST, "No resource based output dataset", s"The specified output dataset '$outputId' is not based on a resource.")
          case None =>
            ErrorResult(BAD_REQUEST, "Output dataset not found", s"The specified output dataset '$outputId' has not been found.")
        }
      case None =>
        ErrorResult(BAD_REQUEST, "No output dataset", "This task does not specify an output dataset.")
    }
  }

  /**
    * Request to copy a task to another project.
    */
  case class CopyTaskRequest(dryRun: Option[Boolean], targetProject: String) {

    def copy(sourceProject: String, taskName: String)
            (implicit userContext: UserContext): CopyTaskResponse = {
      val sourceProj = WorkspaceFactory().workspace.project(sourceProject)
      val targetProj = WorkspaceFactory().workspace.project(targetProject)

      sourceProj.synchronized {
        targetProj.synchronized {
          // Collect all tasks to be copied
          val tasksToCopy = collectTasks(sourceProj, taskName)
          val overwrittenTasks = for(task <- tasksToCopy if targetProj.anyTaskOption(task.id).isDefined) yield task.id.toString
          val copyResources = sourceProj.resources.basePath != targetProj.resources.basePath

          // Copy tasks
          if(!dryRun.contains(true)) {
            for (task <- tasksToCopy) {
              targetProj.updateAnyTask(task.id, task.data, Some(task.metaData))
              // Copy resources
              if(copyResources) {
                for (resource <- task.referencedResources) {
                  targetProj.resources.get(resource.name).writeResource(resource)
                }
              }
            }
          }

          // Generate response
          CopyTaskResponse(tasksToCopy.map(_.id.toString).toSet, overwrittenTasks.toSet)
        }
      }
    }

    /**
      * Returns a task and all its referenced tasks.
      */
    private def collectTasks(project: Project, taskName: Identifier)
                            (implicit userContext: UserContext): Seq[Task[_ <:TaskSpec]] = {
      val task = project.anyTask(taskName)
      Seq(task) ++ task.data.referencedTasks.flatMap(collectTasks(project, _))
    }

  }

  case class CopyTaskResponse(copiedTasks: Set[String], overwrittenTasks: Set[String])

}
