package controllers.projectApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.projectApi.requests.{TaskContextRequest, TaskContextResponse, TaskMetaData}
import controllers.util.SerializationUtils.serializeCompileTime
import controllers.util.{SerializationUtils, TextSearchUtils}
import controllers.workspace.doc.{TaskApiDoc, LegacyDatasetApiDoc => DatasetApiDoc}
import controllers.workspaceApi.projectTask.{ItemCloneRequest, ItemCloneResponse, RelatedItem, RelatedItems}
import controllers.workspaceApi.search.ItemType
import controllers.workspaceApi.search.SearchApiModel.{READ_ONLY, URI_PROPERTY}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{FixedNumberOfInputs, FixedSchemaPort, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetPluginAutoConfigurable, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginDescription}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.MetaDataSerializers.FullTag
import org.silkframework.util.{Identifier, IdentifierUtils}
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.workflow.{WorkflowTaskContext, WorkflowTaskContextInputTask, WorkflowTaskContextOutputTask, WorkflowTaskContextTask}
import org.silkframework.workspace.exceptions.IdentifierAlreadyExistsException
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.util.Try

/**
  * API for project tasks.
  */
@Tag(name = "Project tasks", description = "Access to all tasks in a project.")
class ProjectTaskApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {

  //validate the task id field by ensuring it's unique and corresponds to the right format
  @Operation(
      summary = "Validates custom task ID",
      description = "Receives a custom ID and checks for uniqueness and validity.",
      responses = Array(
        new ApiResponse(
          responseCode = "204",
          description = "The custom ID is both valid and unique.",
        ),
        new ApiResponse(
          responseCode = "400",
          description = "The custom ID is not valid.",
        ),
        new ApiResponse (
          responseCode = "409",
          description = "The custom ID isn't unique, i.e, there is an existing task in the same project with the same ID.",
        )
      )
    )
  def validateIdentifier(@Parameter(
                          name = "projectId",
                          description = "The project identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                         )
                         projectId: String,
                         @Parameter(
                           name = "identifier",
                           description = "the custom task id set by the user",
                           required = true,
                           in = ParameterIn.QUERY,
                           schema = new Schema(implementation = classOf[String])
                         )
                         taskIdentifier: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    val taskId = Try(Identifier(taskIdentifier)).fold(
      ex =>
        throw new BadUserInputException("Invalid identifier", Some(ex)),
      id => id)
    if(project.allTasks.exists(_.id == taskId)) {
      throw IdentifierAlreadyExistsException(s"Task name '$taskIdentifier' is not unique as there is already a task in project '${projectId}' with this name.")
    }
    NoContent
  }


  /** Fetch all related items (tasks) for a specific project task. */
  @Operation(
    summary = "Related items",
    description = "Fetches all directly related items of a project task. Related items are all project tasks that either are directly referenced by the task itself or reference the task. Also any task from a workflow that is directly connected to this task, i.e. either input or output, is part of the result list.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ProjectTaskApi.relatedItemsExampleJson))
        ))
      )
  ))
  def relatedItems(@Parameter(
                     name = "projectId",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectId: String,
                   @Parameter(
                     name = "taskId",
                     description = "The task identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   taskId: String,
                   @Parameter(
                     name = "textQuery",
                     description = "An optional (multi word) text query to filter the list of plugins. Each word in the query has to match at least one sub-string from the label or the type property of a related item.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   textQuery: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectId)
    val task = project.anyTask(taskId)
    val relatedTasks = (task.data.referencedTasks.toSeq ++ task.findDependentTasks(recursive = false).toSeq ++ task.findRelatedTasksInsideWorkflows().toSeq).distinct.
        flatMap(id => project.anyTaskOption(id))
    val relatedItems = relatedTasks map { task =>
      val pd = PluginDescription.forTask(task)
      val itemType = ItemType.itemType(task)
      val itemLinks = ItemType.itemTypeLinks(itemType, projectId, task.id, Some(task.data))
      val readOnly = {
        task.data match {
          case ds: GenericDatasetSpec =>
            Some(ds.readOnly)
          case _ =>
            None
        }
      }
      RelatedItem(task.id, task.fullLabel, task.metaData.description, itemType.label, itemLinks, pd.label, task.tags().map(FullTag.fromTag),
        task.searchTags(PluginContext.fromProject(project)),
        Some(pd.id),
        Some(project.id),
        readOnly
      )
    }
    val filteredItems = filterRelatedItems(relatedItems, textQuery)
    val total = relatedItems.size
    val result = RelatedItems(total, filteredItems)
    Ok(Json.toJson(result))
  }

  /** Returns a list of all relevant UI links for a task. */
  @Operation(
    summary = "Item links",
    description = "All relevant links of this project task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ProjectTaskApi.itemLinksJsonExample))
        ))
      )
    ))
  def itemLinks(@Parameter(
                  name = "projectId",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectId: String,
                @Parameter(
                  name = "taskId",
                  description = "The task identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                taskId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val task = anyTask(projectId, taskId)
    val itemType = ItemType.itemType(task)
    val itemLinks = ItemType.itemTypeLinks(itemType, projectId, task.id, Some(task.data))
    Ok(Json.toJson(itemLinks))
  }

  /** Returns frontend related information about this task, e.g. item type. */
  @Operation(
    summary = "Item info",
    description = "Frontend relevant information about a project task item, e.g. the item type of a task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject("{ \"itemType\": { \"id\": \"linking\", \"label\": \"Linking\" } }"))
        ))
      )
    ))
  def itemInfo(@Parameter(
                name = "projectId",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectId: String,
              @Parameter(
                name = "taskId",
                description = "The task identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              taskId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val projectTask = anyTask(projectId, taskId)
    val itemType = ItemType.itemType(projectTask)
    Ok(Json.obj(
      "itemType" -> Json.obj(
        "id" -> itemType.id,
        "label" -> itemType.label
      )
    ))
  }

  /** Clones an existing task in the project. */
  @Operation(
    summary = "Clone project task",
    description = "Clones an existing project task.",
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The generated ID and the link to the task details page.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ItemCloneResponse]),
          examples = Array(new ExampleObject("{ \"id\": \"200a2458-8cd5-4ca1-8047-b2578aa03d24_Newtask\", \"detailsPage\": \"/workbench/projects/cmem/transform/200a2458-8cd5-4ca1-8047-b2578aa03d24_Newtask\" }"))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    ))
  @RequestBody(
    description = "The request body contains the meta data of the newly created, cloned task. The label is required and must not be empty. The description is optional.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[ItemCloneRequest]),
        examples = Array(new ExampleObject("{ \"metaData\": { \"label\": \"New task\", \"description\": \"Optional description\" } }"))
      ))
  )
  def cloneTask(@Parameter(
                  name = "projectId",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectId: String,
                @Parameter(
                  name = "taskId",
                  description = "The task identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                taskId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>implicit userContext =>
    validateJson[ItemCloneRequest] { request =>
      val label = request.metaData.label.trim
      if(label == "") {
        throw BadUserInputException(  "The label must not be empty!")
      }

      val newTaskId: String = request.newTaskId match {
        case Some(value) if value.nonEmpty => value // Use the non-empty string
        case _ => IdentifierUtils.generateProjectId(label).toString // Handle empty string or None case
      }

      val (project, fromTask) = projectAndAnyTask(projectId, taskId)
      // Clone task spec, since task specs may contain state, e.g. RDF file dataset
      implicit val context: PluginContext = PluginContext.fromProject(project)
      val clonedTaskSpec = Try(fromTask.data.withParameters(ParameterValues.empty)).getOrElse(fromTask.data)
      val requestMetaData = request.metaData.asMetaData
      project.addAnyTask(newTaskId, clonedTaskSpec, requestMetaData.copy(tags = requestMetaData.tags ++ fromTask.metaData.tags))
      val itemType = ItemType.itemType(fromTask)
      val taskLink = ItemType.itemDetailsPage(itemType, projectId, newTaskId).path
      Created(Json.toJson(ItemCloneResponse(newTaskId, taskLink)))
    }
  }

  @Operation(
    summary = "Auto-configure dataset",
    description = "Retrieve an auto-configured version of the dataset that was send with the request.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleJson))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetExampleXml))
          )
        )
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the dataset type does not support auto-configuration."
      )
    )
  )
  def postDatasetAutoConfigured(@Parameter(
                                  name = "projectId",
                                  description = "The project identifier",
                                  required = true,
                                  in = ParameterIn.PATH,
                                  schema = new Schema(implementation = classOf[String])
                                )
                                projectId: String): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectId)
    implicit val context: PluginContext = PluginContext.fromProject(project)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, user = userContext)
    SerializationUtils.deserializeCompileTime[GenericDatasetSpec]() { datasetSpec =>
      val datasetPlugin = datasetSpec.plugin
      datasetPlugin match {
        case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
          val autoConfDataset = autoConfigurable.autoConfigured
          serializeCompileTime[GenericDatasetSpec](DatasetSpec[Dataset](autoConfDataset, datasetSpec.uriAttribute), Some(project))
        case _ =>
          ErrorResult(BadUserInputException("This dataset type does not support auto-configuration."))
      }
    }
  }

  @Operation(
    summary = "Task context",
    description = "Retrieves additional information to the given task context.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TaskApiDoc.taskContextResponse))
          )
        )
      )
    )
  )
  @RequestBody(
    description = "The request body contains the task context for which additional information is requested.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[TaskContextRequest]),
        examples = Array(new ExampleObject(TaskApiDoc.taskContextRequest))
      ))
  )
  def taskContext(@Parameter(
                    name = "projectId",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[TaskContextRequest] { request =>
      val project = getProject(projectId)
      val task = project.anyTask(request.taskId)
      val inOutTasks = ProjectTaskApi.validateTaskContext(project, Some(request.taskContext))
      val inputTasks: Seq[TaskMetaData] = inOutTasks.inputTasks.map(inputTask => {
        val taskSpec = inputTask.task.data
        val hasSchema = taskSpec.outputPort.exists(_.schemaOpt.isDefined)
        TaskMetaData(inputTask.workflowContextTask.id, inputTask.task.fullLabel, inputTask.task.data.isInstanceOf[DatasetSpec[_]], hasSchema)
      })
      val outputTasks: Seq[TaskMetaData] = inOutTasks.outputTasks.map(outputTask => {
        val taskSpec = outputTask.task.data
        var hasSchema = outputTask.workflowContextTask.configPort
        val inputPort = outputTask.workflowContextTask.inputPort
        taskSpec.inputPorts match {
          case FixedNumberOfInputs(ports) if !hasSchema && inputPort.isDefined && inputPort.get >= 0 && inputPort.get < ports.size =>
            hasSchema = ports(inputPort.get).schemaOpt.isDefined
          case _ =>
        }
        TaskMetaData(outputTask.workflowContextTask.id, outputTask.task.fullLabel, outputTask.task.data.isInstanceOf[DatasetSpec[_]], hasSchema)
      })
      val originalInputs = inputTaskIds(task).map(configuredInputs => configuredInputs == inputTasks.map(_.taskId))
      val originalOutputs = outputTaskIds(task).map(configuredOutputs => configuredOutputs == outputTasks.map(_.taskId))
      Ok(Json.toJson(TaskContextResponse(inputTasks, outputTasks, originalInputs, originalOutputs)))
    }
  }

  private def inputTaskIds(task: Task[_ <: TaskSpec]): Option[Seq[String]] = {
    task.data match {
      case transform: TransformSpec =>
        Some(Seq(transform.selection.inputId.toString))
      case linking: LinkSpec =>
        Some(linking.dataSelections.map(_.inputId.toString).toSeq)
      case _ =>
        None
    }
  }

  private def outputTaskIds(task: Task[_ <: TaskSpec]): Option[Seq[String]] = {
    task.data match {
      case transform: TransformSpec =>
        transform.output.map(o => Seq(o.toString))
      case linking: LinkSpec =>
        linking.output.map(o => Seq(o.toString))
      case _ =>
        None
    }
  }

  private def filterRelatedItems(relatedItems: Seq[RelatedItem], textQuery: Option[String]): Seq[RelatedItem] = {
    val searchWords =  TextSearchUtils.extractSearchTerms(textQuery.getOrElse(""))
    if(searchWords.isEmpty) {
      relatedItems
    } else {
      relatedItems.filter(relatedItem => // Description is not displayed, so don't search in description.
        TextSearchUtils.matchesSearchTerm(
          searchWords,
          s"${relatedItem.label} ${relatedItem.`type`} ${relatedItem.pluginLabel} ${relatedItem.tags.map(_.label).mkString(" ")} ${relatedItem.searchTags.mkString(" ")}".toLowerCase
        )
      )
    }
  }
}

object ProjectTaskApi {

  private final val relatedItemsExampleJson =
"""
{
    "total": 2,
    "items": [
        {
            "id": "testCsv",
            "itemLinks": [
                {
                    "label": "Dataset details page",
                    "path": "/workspaceNew/projects/testTasks/dataset/testCsv"
                }
            ],
            "label": "test Csv",
            "type": "Dataset",
            "pluginLabel": "CSV"
        },
        {
            "id": "workflow",
            "itemLinks": [
                {
                    "label": "Workflow details page",
                    "path": "/workspaceNew/projects/testTasks/workflow/workflow"
                },
                {
                    "label": "Workflow editor",
                    "path": "/workflow/editor/project/workflow"
                }
            ],
            "label": "workflow",
            "type": "Workflow",
            "pluginLabel": "Workflow"
        }
    ]
}
"""

  private final val itemLinksJsonExample =
"""
[
    {
        "label": "Transform details page",
        "path": "/workbench/projects/cmem/transform/someTransformTask"
    },
    {
        "label": "Mapping editor",
        "path": "/transform/cmem/someTransformTask/editor"
    },
    {
        "label": "Transform evaluation",
        "path": "/transform/cmem/someTransformTask/evaluate"
    },
    {
        "label": "Transform execution",
        "path": "/transform/cmem/someTransformTask/execute"
    }
]
"""

  /** Validates a workflow context
    *
    * @param project             The project the workflow is in.
    * @param workflowTaskContext The workflow context that should be validated
    */
  def validateTaskContext(project: Project,
                          workflowTaskContext: Option[WorkflowTaskContext])
                         (implicit userContext: UserContext): InputAndOutputTasks = {
    var inputTasks: Seq[(WorkflowTaskContextInputTask, Task[_ <: TaskSpec])] = Seq.empty
    var outputTasks: Seq[(WorkflowTaskContextOutputTask, Task[_ <: TaskSpec])] = Seq.empty
    workflowTaskContext.foreach(c => {
      def checkTasks[T <: WorkflowTaskContextTask](tasks: Option[Seq[T]]): Seq[(T, Task[_ <: TaskSpec])] = {
        tasks match {
          case Some(ts) =>
            ts.map(t => (t, project.anyTask(t.id)))
          case None => Seq.empty
        }
      }

      inputTasks = checkTasks(c.inputTasks)
      outputTasks = checkTasks(c.outputTasks)
    })
    InputAndOutputTasks(inputTasks.map(t => InputTask(t._1, t._2)), outputTasks.map(t => OutputTask(t._1, t._2)))
  }

  case class InputTask(workflowContextTask: WorkflowTaskContextInputTask,
                       task: Task[_ <: TaskSpec])

  case class OutputTask(workflowContextTask: WorkflowTaskContextOutputTask,
                        task: Task[_ <: TaskSpec])

  private def mergeLabeledPaths(labeledPaths: Seq[Seq[LabeledPath]]): Seq[LabeledPath] = {
    labeledPaths.flatten.distinct
  }

  case class LabeledPath(path: Path, label: Option[String])

  case class InputAndOutputTasks(inputTasks: Seq[InputTask],
                                 outputTasks: Seq[OutputTask]) {
    private def entitySchemaToLabeledPaths(entitySchema: EntitySchema): Seq[LabeledPath] = {
      entitySchema.typedPaths.map(tp => LabeledPath(tp, None))
    }

    private def inputPaths(): Option[Seq[TypedPath]] = {
      if (inputTasks.isEmpty) {
        None
      } else {
        val typedPaths = inputTasks.map(inputTask => {
          inputTask.task.data.outputPort match {
            case Some(FixedSchemaPort(schema)) =>
              schema.typedPaths
            case _ =>
              Seq.empty
          }
        })
        Some(typedPaths.flatten.distinct)
      }
    }

    def inputEntitySchema(): Option[EntitySchema] = {
      inputPaths()
        .map(ps => {
          EntitySchema("", ps.toIndexedSeq)
        })
    }

    def labeledOutputPaths(): Option[Seq[LabeledPath]] = {
      if (outputTasks.isEmpty) {
        None
      } else {
        val labeledPaths = outputTasks.map(outputTask => {
          if (outputTask.workflowContextTask.configPort) {
            val pd = PluginDescription.forTask(outputTask.task)
            val parameters = pd.parameters.filter(_.visibleInDialog)
            parameters.map(p => LabeledPath(UntypedPath(p.name), Some(p.label)))
          } else {
            val port = outputTask.workflowContextTask.inputPort.get
            outputTask.task.data.inputPorts match {
              case FixedNumberOfInputs(ports) if port < ports.size =>
                entitySchemaToLabeledPaths(ports(port).schemaOpt.getOrElse(EntitySchema.empty))
              case _ => Seq.empty
            }
          }
        })
        Some(mergeLabeledPaths(labeledPaths))
      }
    }
  }
}
