package controllers.workspace

import java.util.logging.Logger

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.SerializationUtils
import javax.inject.Inject
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.ResourceBasedDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterAutoCompletion, PluginDescription, PluginObjectParameterTypeTrait}
import org.silkframework.runtime.resource.FileResource
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{JsonSerialization, JsonSerializers}
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class TaskApi @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with ControllerUtilsTrait {

  implicit private lazy val executionContext: ExecutionContext = controllerComponents.executionContext
  private val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)

  def postTask(projectName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      project.addAnyTask(task.id, task.data, task.metaData)
      implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue](prefixes = project.config.prefixes, projectId = Some(project.name))
      Created(JsonSerializers.GenericTaskJsonFormat.write(task)).
          withHeaders(LOCATION -> routes.TaskApi.getTask(projectName, task.id).path())
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

  def patchTask(projectName: String, taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
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

  def getTask(projectName: String, taskName: String, withLabels: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    accessMonitor.saveProjectTaskAccess(project.config.id, task.id)
    if(withLabels) {
      getTaskWithParameterLabels(projectName, project, task)
    } else {
      SerializationUtils.serializeCompileTime[Task[TaskSpec]](task, Some(project))
    }
  }

  // Add parameter value labels for auto-completable parameters, e.g. task label of a task reference parameter.
  private def getTaskWithParameterLabels(projectName: String,
                                         project: Project,
                                         task: ProjectTask[_ <: TaskSpec])
                                        (implicit userContext: UserContext): Result = {
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue](prefixes = project.config.prefixes, projectId = Some(project.config.id))
    // JSON only
    val jsObj: JsObject = JsonSerialization.toJson[Task[TaskSpec]](task).as[JsObject]
    val data = (jsObj \ DATA).as[JsObject]
    val parameters = (data \ PARAMETERS).as[JsObject]
    val parameterValue = parameters.value
    val updatedParameters: JsObject = parametersWithLabel(projectName, task, parameterValue)
    val updatedDataFields = data.fields ++ Seq(PARAMETERS -> updatedParameters)
    val updatedData = JsObject(updatedDataFields)
    val updatedJsObj = JsObject(jsObj.fields.filterNot(_._1 == DATA) ++ Seq(DATA -> updatedData))
    Ok(updatedJsObj)
  }

  /** Changes the value format from "param": <VALUE> to "param": {"value": <VALUE>, "label": "<LABEL>"}. The label is optional.
    * This will be applied recursively for nested parameter types.
    */
  private def parametersWithLabel(projectName: String,
                                  task: ProjectTask[_ <: TaskSpec],
                                  parameterValues: collection.Map[String, JsValue])
                                 (implicit userContext: UserContext): JsObject = {
    try {
      val pluginClass = task.data match {
        case ds: GenericDatasetSpec => ds.plugin.getClass
        case o: TaskSpec => o.getClass
      }
      val pluginDescription = PluginDescription(pluginClass)
      JsObject(addLabelsToValues(projectName, parameterValues, pluginDescription))
    } catch {
      case NonFatal(ex) =>
        log.warning(s"Could not get labels of plugin parameters for task '${task.taskLabel()}' in project '$projectName'. Details: " + ex.getMessage)
        JsObject(parameterValues)
    }
  }

  // Adds labels to parameter values of nested objects. This is guaranteed to only go one level deep, since objects presented in the UI are not allowed to be nested multiple levels.
  private def addLabelsToValues(projectName: String,
                                parameterValues: collection.Map[String, JsValue],
                                pluginDescription: PluginDescription[_])
                               (implicit userContext: UserContext): Map[String, JsValue] = {
    val parameterDescription = pluginDescription.parameters.map(p => (p.name, p)).toMap
    val updatedParameters = for((parameterName, parameterValue) <- parameterValues) yield {
      val pd = parameterDescription.getOrElse(parameterName,
        throw new RuntimeException(s"Parameter '$parameterName' is not part of the parameter description of plugin '${pluginDescription.id}'."))
      val updatedValue = parameterValue match {
        case valueObj: JsObject if pd.visibleInDialog && pd.parameterType.isInstanceOf[PluginObjectParameterTypeTrait] =>
          val paramPluginDescription = PluginDescription(pd.parameterType.asInstanceOf[PluginObjectParameterTypeTrait].pluginObjectParameterClass)
          val updatedInnerValues = addLabelsToValues(projectName, valueObj.value, paramPluginDescription)
          JsObject(Seq("value" -> JsObject(updatedInnerValues))) // Nested objects cannot have a label
        case jsString: JsString if pd.autoCompletion.isDefined && pd.autoCompletion.get.autoCompleteValueWithLabels && jsString.value != "" =>
          val autoComplete = pd.autoCompletion.get
          val dependsOnParameterValues = fetchDependsOnValues(autoComplete, parameterValues)
          val label = autoComplete.autoCompletionProvider.valueToLabel(projectName, jsString.value, dependsOnParameterValues, workspace)
          JsObject(Seq("value" -> jsString) ++ label.toSeq.map(l => "label" -> JsString(l)))
        case other: JsValue =>
          JsObject(Seq("value" -> other))
      }
      (parameterName, updatedValue)
    }
    updatedParameters.toMap
  }

  private def fetchDependsOnValues(autoComplete: ParameterAutoCompletion,
                                   parameterValues: collection.Map[String, JsValue]) = {
    autoComplete.autoCompletionDependsOnParameters.map { param =>
      parameterValues.getOrElse(param,
        throw new RuntimeException(s"No value found for plugin parameter '$param'. Could not retrieve label!")).asOpt[String] match {
        case Some(value) => value
        case None => throw new RuntimeException(s"Value of dependsOn parameter '${param}' is not a String based parameter.")
      }
    }
  }

  def deleteTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def putTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    implicit val readContext: ReadContext = ReadContext()

    SerializationUtils.deserializeCompileTime[MetaData](defaultMimeType = "application/json") { metaData =>
      task.updateMetaData(metaData)
      Ok(taskMetaDataJson(task))
    }
  }

  def getTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val metaDataJson = taskMetaDataJson(task)
    accessMonitor.saveProjectTaskAccess(project.config.id, task.id)
    Ok(metaDataJson)
  }

  // Task meta data object as JSON
  private def taskMetaDataJson(task: ProjectTask[_ <: TaskSpec])(implicit userContext: UserContext): JsObject = {
    val formatOptions =
      TaskFormatOptions(
        includeMetaData = Some(false),
        includeTaskData = Some(false),
        includeTaskProperties = Some(false),
        includeRelations = Some(true),
        includeSchemata = Some(true)
      )
    val taskFormat = new TaskJsonFormat(formatOptions, Some(userContext))(TaskSpecJsonFormat)
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue](projectId = Some(task.project.config.id))
    val taskJson = taskFormat.write(task)
    val metaDataJson = JsonSerializers.toJson(task.metaData)
    val mergedJson = metaDataJson.as[JsObject].deepMerge(taskJson.as[JsObject])
    mergedJson
  }

  def cloneTask(projectName: String, oldTask: String, newTask: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.addAnyTask(newTask, project.anyTask(oldTask))
    Ok
  }

  def copyTask(projectName: String,
               taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
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
    val cachesLoaded = task.activities.filter(_.autoRun).forall(!_.status().isRunning)

    Ok(JsBoolean(cachesLoaded))
  }

  def downloadOutput(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    task.data.outputTasks.headOption match {
      case Some(outputId) =>
        project.taskOption[GenericDatasetSpec](outputId).map(_.data.plugin) match {
          case Some(ds: ResourceBasedDataset) =>
            ds.file match {
              case FileResource(file) =>
                Ok.sendFile(file)
              case _ =>
                ErrorResult(BAD_REQUEST, "Output resource is not a file", s"The specified output dataset '$outputId' is not based on a file resource.")
            }
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
