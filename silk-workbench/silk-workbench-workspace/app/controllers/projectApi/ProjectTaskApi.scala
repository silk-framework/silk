package controllers.projectApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.TextSearchUtils
import controllers.workspaceApi.IdentifierUtils
import controllers.workspaceApi.projectTask.{ItemCloneRequest, ItemCloneResponse, RelatedItem, RelatedItems}
import controllers.workspaceApi.search.ItemType
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.util.Try

/**
  * API for project tasks.
  */
@Tag(name = "Project tasks", description = "Access to all tasks in a project.")
class ProjectTaskApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {

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
    val relatedTasks = (task.data.referencedTasks.toSeq ++ task.findDependentTasks(recursive = false).toSeq ++ task.findRelatedTasksInsideWorkflows.toSeq).distinct.
        flatMap(id => project.anyTaskOption(id))
    val relatedItems = relatedTasks map { task =>
      val pd = PluginDescription(task)
      val itemType = ItemType.itemType(task)
      val itemLinks = ItemType.itemTypeLinks(itemType, projectId, task.id, Some(task.data))
      RelatedItem(task.id, task.fullLabel, task.metaData.description, itemType.label, itemLinks, pd.label)
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
        throw BadUserInputException("The label must not be empty!")
      }
      val generatedId = IdentifierUtils.generateProjectId(label)
      val (project, fromTask) = projectAndAnyTask(projectId, taskId)
      // Clone task spec, since task specs may contain state, e.g. RDF file dataset
      implicit val resourceManager: ResourceManager = project.resources
      implicit val prefixes: Prefixes = project.config.prefixes
      val clonedTaskSpec = Try(fromTask.data.withProperties(Map.empty)).getOrElse(fromTask.data)
      project.addAnyTask(generatedId, clonedTaskSpec, request.metaData.asMetaData)
      val itemType = ItemType.itemType(fromTask)
      val taskLink = ItemType.itemDetailsPage(itemType, projectId, generatedId).path
      Created(Json.toJson(ItemCloneResponse(generatedId, taskLink)))
    }
  }

  private def filterRelatedItems(relatedItems: Seq[RelatedItem], textQuery: Option[String]): Seq[RelatedItem] = {
    val searchWords =  TextSearchUtils.extractSearchTerms(textQuery.getOrElse(""))
    if(searchWords.isEmpty) {
      relatedItems
    } else {
      relatedItems.filter(relatedItem => // Description is not displayed, so don't search in description.
        TextSearchUtils.matchesSearchTerm(searchWords, s"${relatedItem.label} ${relatedItem.`type`} ${relatedItem.pluginLabel}".toLowerCase))
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
}
