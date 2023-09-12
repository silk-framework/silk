package controllers.workspace

import config.WorkbenchLinks
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils
import controllers.workspace.doc.TaskApiDoc
import controllers.workspace.taskApi.{TaskApiUtils, TaskLink}
import controllers.workspace.workspaceRequests.{CopyTasksRequest, CopyTasksResponse}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TaskFormatOptions, TaskJsonFormat, TaskSpecJsonFormat, fromJson, metaData, toJson, _}
import org.silkframework.serialization.json.MetaDataSerializers._
import org.silkframework.serialization.json.{JsonSerialization, JsonSerializers}
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc._

import java.util.logging.Logger
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

@Tag(name = "Project tasks")
class TaskApi @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  implicit private lazy val executionContext: ExecutionContext = controllerComponents.executionContext
  private val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)

  @Operation(
    summary = "Add task",
    description = " Add a new task to the project. If the 'id' parameter is omitted in the request, an ID will be generated from the label – which is then required.",
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The added task.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(`type` = "object"),
          examples = Array(new ExampleObject(TaskApiDoc.taskExampleJson))
        ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided task specification is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      ),
      new ApiResponse(
        responseCode = "409",
        description = "If a task with the given identifier already exists."
      )
    ))
  @RequestBody(
    description = "The task description",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(TaskApiDoc.taskExampleJson))
      ))
  )
  def postTask(@Parameter(
                 name = "project",
                 description = "The project identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               projectName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      project.addAnyTask(task.id, task.data, task.metaData)
      implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
      Created(JsonSerializers.GenericTaskJsonFormat.write(task)).
          withHeaders(LOCATION -> routes.TaskApi.getTask(projectName, task.id).path())
    }
  }

  @Operation(
    summary = "Add or update task",
    description = "Add or update a task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the task has been added or updated successfully."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided task specification is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    ))
  @RequestBody(
    description = "The task description",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(TaskApiDoc.taskExampleJson))
      ))
  )
  def putTask(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "task",
                description = "The task identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    SerializationUtils.deserializeCompileTime[Task[TaskSpec]]() { task =>
      if(task.id.toString != taskName) {
        throw new BadUserInputException(s"Inconsistent task identifiers: Got $taskName in URL, but ${task.id} in payload.")
      }
      project.updateAnyTask(task.id, task.data, Some(task.metaData))
      Ok
    }
  }

  @Operation(
    summary = "Update task",
    description = "Update selected properties of a task. Only the sent JSON paths will be updated, i.e., the provided JSON is deep merged into the existing task JSON.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the task has been updated successfully."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided task specification is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    ))
  @RequestBody(
    description = "The task description",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject("{ \"metadata\": { \"description\": \"task description\" } }"))
      ))
  )
  def patchTask(@Parameter(
                  name = "project",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectName: String,
                @Parameter(
                  name = "task",
                  description = "The task identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    // Load current task
    val project = WorkspaceFactory().workspace.project(projectName)
    val currentTask = project.anyTask(taskName)

    // Update task JSON
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    val currentJson = toJson[Task[TaskSpec]](currentTask).as[JsObject]
    // Templates are only partially defined, so they must be replaced completely by the new value.
    // Since template values have precedence over the parameter values, the parameter values can be deep merged without problems.
    val currentJsonWithoutTemplates = currentJson ++ Json.obj(
      "data" -> currentJson.value.get("data").map(data =>
        data.as[JsObject] - "templates")
    )
    val updatedJson = currentJsonWithoutTemplates.deepMerge(request.body.as[JsObject])

    // Update task
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    val updatedTask = fromJson[Task[TaskSpec]](updatedJson)
    if(updatedTask.id.toString != taskName) {
      throw new BadUserInputException(s"Inconsistent task identifiers: Got $taskName in URL, but ${updatedTask.id} in payload.")
    }
    project.updateAnyTask(updatedTask.id, updatedTask.data, Some(updatedTask.metaData))

    Ok
  }

  @Operation(
    summary = "Retrieve task",
    description = "Retrieve a task from a project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The task.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(
            new ExampleObject(name = "Without labels", value = TaskApiDoc.taskExampleWithoutLabelsJson),
            new ExampleObject(name = "With labels", value = TaskApiDoc.taskMetadataExampleWithLabelsJson)
          )
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  def getTask(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "task",
                description = "The task identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              taskName: String,
              @Parameter(
                name = "withLabels",
                description = "If true, all parameter values will be reified in a new object that has an optional label property. A label is added for all auto-completable parameters that have the 'autoCompleteValueWithLabels' property set to true. This guarantees that a user always sees the label of such values. For object type parameters that have set the 'visibleInDialog' flag set to true, this reification is done on all levels. For object type parameters that should not be shown in UI dialogs this is still done for the first level of the task itself, but not deeper. These values should never be set or updated by a normal UI dialog anyway and should be ignored by a task dialog.",
                required = false,
                in = ParameterIn.QUERY,
                schema = new Schema(implementation = classOf[Boolean])
              )
              withLabels: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
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
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    // JSON only
    val jsObj: JsObject = JsonSerialization.toJson[Task[TaskSpec]](task).as[JsObject]
    val data = (jsObj \ DATA).as[JsObject]
    val parameters = (data \ PARAMETERS).as[JsObject]
    val parameterValue = parameters.value
    val updatedParameters: JsObject = TaskApiUtils.parametersWithLabel(projectName, task, parameterValue)
    val updatedDataFields = data.fields ++ Seq(PARAMETERS -> updatedParameters)
    val updatedData = JsObject(updatedDataFields)
    val updatedJsObj = JsObject(jsObj.fields.filterNot(_._1 == DATA) ++ Seq(DATA -> updatedData))
    Ok(updatedJsObj)
  }

  @Operation(
    summary = "Delete task",
    description = "Remove a task from a project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the task has been deleted or there is no task with that identifier."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    )
  )
  def deleteTask(@Parameter(
                  name = "project",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectName: String,
                @Parameter(
                  name = "task",
                  description = "The task identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                taskName: String,
                @Parameter(
                  name = "removeDependentTasks",
                  description = "If true, all tasks that directly or indirectly reference this task are removed as well.",
                  required = true,
                  in = ParameterIn.QUERY,
                  schema = new Schema(implementation = classOf[Boolean])
                )
                removeDependentTasks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  @Operation(
    summary = "Update task metadata",
    description = "Updates task metadata that includes user metadata, such as the task label as well as technical metadata, such as the referenced tasks.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the task metadata has been updated successfully."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  @RequestBody(
    description = "Updated meta data.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[MetaDataPlain]),
        examples = Array(new ExampleObject("{ \"label\": \"New label\", \"description\": \"New description\" }"))
      ))
  )
  def putTaskMetadata(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)

    validateJson[MetaDataPlain] { metaData =>
      task.updateMetaData(metaData.toMetaData)
      Ok(taskMetaDataJson(task, None))
    }
  }

  @Operation(
    summary = "Retrieve task metadata",
    description = "Retrieve task metadata that includes user metadata, such as the task label as well as technical metadata, such as the referenced tasks.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The task metadata",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TaskApiDoc.taskMetadataExampleJson))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  def getTaskMetadata(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      taskName: String,
                      @Parameter(
                        name = "withTaskLinks",
                        description = """If set to true dependent tasks are returned in the form {id: "", label: "", taskLink: "/workbench/projects/..."}.""",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[Boolean])
                      )
                      withTaskLinks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val metaDataJson = taskMetaDataJson(task, if(withTaskLinks) Some(dependentTaskLinkFormatter(project)) else None)
    accessMonitor.saveProjectTaskAccess(project.config.id, task.id)
    Ok(metaDataJson)
  }

  private def dependentTaskLinkFormatter(project: Project)
                                        (implicit userContext: UserContext): String => JsValue = (taskId: String) => {
    val task = project.anyTask(taskId)
    Json.toJson(TaskLink(task.id, task.metaData.label, WorkbenchLinks.editorLink(task)))
  }

  @Operation(
    summary = "Retrieve expanded task metadata",
    description = "Metadata of the task, such as the label and description.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[MetaDataExpanded])
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    ))
  def getTaskMetadataExpanded(@Parameter(
                                 name = "project",
                                 description = "The project identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               projectId: String,
                               @Parameter(
                                 name = "task",
                                 description = "The task identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               taskId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectId)
    val task = project.anyTask(taskId)
    Ok(Json.toJson(MetaDataExpanded.fromMetaData(task.metaData, project.tagManager)))
  }

  /** Task meta data object as JSON
    *
    * @param dependentTaskFormatter Converts dependent tasks to a different JSON format than the string ID.
    */
  private def taskMetaDataJson(task: ProjectTask[_ <: TaskSpec],
                               dependentTaskFormatter: Option[String => JsValue])(implicit userContext: UserContext): JsObject = {
    val formatOptions =
      TaskFormatOptions(
        includeMetaData = Some(false),
        includeTaskData = Some(false),
        includeTaskProperties = Some(false),
        includeRelations = Some(true),
        includeSchemata = Some(true)
      )
    val taskFormat = new TaskJsonFormat[TaskSpec](formatOptions, Some(userContext), dependentTaskFormatter)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(task.project)
    val taskJson = taskFormat.write(task)
    val metaDataJson = JsonSerializers.toJson(task.metaData)
    val mergedJson = metaDataJson.as[JsObject].deepMerge(taskJson.as[JsObject])
    mergedJson
  }

  @Operation(
    summary = "Clone Task",
    description = "Clone a task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the has been cloned."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  def cloneTask(@Parameter(
                  name = "project",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectName: String,
                @Parameter(
                  name = "task",
                  description = "The identifier of the task to be cloned",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                oldTask: String,
                @Parameter(
                  name = "newTask",
                  description = "The new task identifier",
                  required = true,
                  in = ParameterIn.QUERY,
                  schema = new Schema(implementation = classOf[String])
                )
                newTask: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val fromTask = project.anyTask(oldTask)
    // Clone task spec, since task specs may contain state, e.g. RDF file dataset
    implicit val context: PluginContext = PluginContext.fromProject(project)
    val clonedTaskSpec = Try(fromTask.data.withParameters(ParameterValues.empty)).getOrElse(fromTask.data)
    project.addAnyTask(newTask, clonedTaskSpec, MetaData.empty.copy(tags = fromTask.metaData.tags))
    Ok
  }

  @Operation(
    summary = "Copy Task to Another Project",
    description = "Copies a task to another project. All tasks that the copied task references (directly or indirectly) are copied as well. Referenced resources are copied only if the target project uses a different resource path than the source project. Using the dryRun attribute, a copy operation can be simulated, i.e., the response listing the tasks to be copied and overwritten can be checked first.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the has been copied.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[CopyTasksResponse]),
            examples = Array(new ExampleObject(TaskApiDoc.copyTaskResponseJsonExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  @RequestBody(
    description = "The copy task request.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[CopyTasksRequest]),
        examples = Array(new ExampleObject(TaskApiDoc.copyTaskRequestJsonExample))
      ))
  )
  def copyTask(@Parameter(
                 name = "project",
                 description = "The project identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               projectName: String,
               @Parameter(
                 name = "task",
                 description = "The task identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[CopyTasksRequest] { copyRequest =>
      val result = copyRequest.copyTask(projectName, taskName)
      Ok(Json.toJson(result))
    }
  }

  def cachesLoaded(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val cachesLoaded = task.activities.filter(_.autoRun).forall(!_.status().isRunning)

    Ok(JsBoolean(cachesLoaded))
  }

}


