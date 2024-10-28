package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.SearchApiDoc
import controllers.workspaceApi.search.SearchApiModel._
import controllers.workspaceApi.search.{ItemType, ParameterAutoCompletionRequest}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.{Aggregator, DistanceMeasure}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.MetaDataSerializers.FullTag
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController, Result}

import java.util.logging.Logger
import javax.inject.Inject

@Tag(name = "Search", description = "Search for tasks in the workspace")
class SearchApi @Inject() (implicit accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)
  implicit val autoCompletionResultJsonFormat: Format[AutoCompletionResult] = Json.format[AutoCompletionResult]

  /** Search tasks by text search query. */
  @Operation(
    summary = "Search Tasks",
    description = "List all tasks that fulfill a set of filters. All JSON fields sent in the the request are optional. The request example reflects the default values that are chosen when a field is missing in the request.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Search result",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(SearchApiDoc.searchTasksResponseExample))
        )
      )
    )
  ))
  @RequestBody(
    description = "Search request",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[SearchRequest], example = SearchApiDoc.searchTasksRequestExample)
    ))
  )
  def search(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[SearchRequest] { searchRequest =>
      Ok(searchRequest())
    }
  }

  /** Faceted search API for the workspace search */
  @Operation(
    summary = "Artifact search",
    description = "Allows to search over all DataIntegration artifacts with text search and filter facets.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Search result",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[FacetedSearchResult]),
          examples = Array(new ExampleObject(SearchApiDoc.artifactSearchResponseExample))
        ))
      )
    )
  )
  @RequestBody(
    content = Array(new Content(
      mediaType = "application/json",
      schema = new Schema(implementation = classOf[FacetedSearchRequest]),
      examples = Array(new ExampleObject(SearchApiDoc.artifactSearchRequestExample))
  )))
  def facetedSearch(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[FacetedSearchRequest] { facetedSearchRequest =>
      Ok(facetedSearchRequest())
    }
  }

  /** Recently viewed items of user. */
  @Operation(
    summary = "Recently viewed items",
    description = "Fetch a list of the (max. 50) most recently accessed items, e.g. visited details pages or updated projects or tasks. The list is ordered by recency of the access, more recent coming first.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "List of recently viewed items with:\n  - Project ID and label, optional task ID and label (when the item is a task)\n   - Item type, e.g. 'project', 'transform' etc.\n  - The plugin ID for tasks\n  - Item links of the returned item, e.g. details page etc.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(SearchApiDoc.recentlyViewedItemsExample))
        ))
      )
    ))
  def recentlyViewedItems(): Action[AnyContent] = UserContextAction { implicit userContext =>
    val w = workspace
    val accessedItems = accessMonitor.getAccessItems.reverse
    val projects = accessedItems.map(_.projectId).distinct.flatMap(projectId => w.findProject(projectId))
    val availableProjects = projects.map(p => (p.id.toString, p)).toMap
    val availableItems = accessedItems.filter(item => availableProjects.contains(item.projectId))
    val items = availableItems flatMap { item =>
      val project = availableProjects(item.projectId)
      val taskOpt = item.taskIdOpt.flatMap(taskId => project.anyTaskOption(taskId))
      if(item.taskIdOpt.isDefined && taskOpt.isEmpty) {
        None
      } else {
        val itemType = if(taskOpt.isEmpty) ItemType.project else ItemType.itemType(taskOpt.get.data)
        val additionalData = taskOpt match {
          case Some(task) =>
            val pd = PluginDescription.forTask(task)
            val taskJson =
              Json.obj(
                "taskId" -> JsString(task.id),
                "taskLabel" -> JsString(taskOpt.get.label()),
                PLUGIN_ID -> JsString(pd.id),
                PLUGIN_LABEL -> JsString(pd.label),
                TAGS -> Json.toJson(task.tags().map(FullTag.fromTag)),
                SEARCH_TAGS -> Json.toJson(task.searchTags(task.project.config.prefixes))
              )
            task.data match {
              case ds: GenericDatasetSpec =>
                taskJson + ("readOnly" -> JsBoolean(ds.readOnly))
              case _ =>
                taskJson
            }
          case None =>
            Json.obj(
              TAGS -> Json.toJson(project.tags().map(FullTag.fromTag))
            )
        }
        Some(Json.obj(
          "projectId" -> JsString(item.projectId),
          "projectLabel" -> JsString(project.config.label()),
          "itemType" -> JsString(itemType.id),
          "itemLinks" -> Json.toJson(ItemType.itemTypeLinks(
            itemType,
            project.id,
            taskOpt.map(_.id.toString).getOrElse(project.id.toString),
            taskOpt.map(_.data)
          ))
        ) ++ additionalData)
      }
    }
    Ok(JsArray(items))
  }

  /** Auto-completion service for plugin parameters. */
  @Operation(
    summary = "Plugin parameter auto-completion",
    description = "Auto-completion endpoint for plugin parameter values.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The response contains the result list of matching auto-completion results. The 'label' property is optional and may not be defined, even for parameters that are supposed to have labels. In this case the 'value' should be taken as label.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(SearchApiDoc.parameterAutoCompletionResponseExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404"
      )
    ))
  @RequestBody(
    description =
      """The 'pluginId' and 'parameterId' reference the parameter of a plugin, they values can be read e.g. from the /plugins endpoint.
        The 'projectId' provides the project context for parameters that hold values that are project specific, e.g. task references.
        The 'dependsOnParameterValues' parameter contains all the values of other parameters this auto-completion depends on. E.g. if a
        plugin has a parameter 'project' and 'projectTask', the 'projectTask' parameter may depend on 'project',
        because only when the project is known then the auto-completion of project tasks can be performed. The list
        of parameters are returned in the plugin parameter description.
        The 'textQuery' parameter is a conjunctive multi word query matching against the possible results.
        The 'offset' and 'limit' parameters allow for paging through the result list.
        """,
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[ParameterAutoCompletionRequest]),
        examples = Array(new ExampleObject(SearchApiDoc.parameterAutoCompletionRequestExample))
    ))
  )
  def parameterAutoCompletion(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ParameterAutoCompletionRequest] { request =>
      PluginRegistry.pluginDescriptionsById(
        request.pluginId,
        // Plugin ID collisions exist, we need to filter the types of plugins.
        assignableTo = Some(Seq(
          classOf[TaskSpec],
          classOf[PluginObjectParameter],
          classOf[Dataset],
          classOf[Transformer],
          classOf[DistanceMeasure],
          classOf[Aggregator]
        ))
      ).headOption match {
        case Some(pluginDescription) =>
          parameterAutoCompletion(request, pluginDescription)
        case None =>
          log.warning(s"Requesting auto-completion for non-existing plugin: ${request.pluginId}.")
          NotFound
      }
    }
  }

  private def parameterAutoCompletion(request: ParameterAutoCompletionRequest,
                                      pluginDescription: PluginDescription[_])
                                     (implicit userContext: UserContext): Result = {
    val parameter = pluginDescription.findParameter(request.parameterId)
    parameter.autoCompletion match {
      case Some(autoCompletion) =>
        parameterAutoCompletion(request, autoCompletion, pluginDescription)
      case None =>
        val error = s"Parameter '${parameter.name}' of plugin '${request.pluginId}' has no auto-completion support."
        log.warning(error)
        NotFound(error)
    }
  }

  private def parameterAutoCompletion(request: ParameterAutoCompletionRequest,
                                      autoCompletion: ParameterAutoCompletion,
                                      pluginDescription: PluginDescription[_])
                                     (implicit userContext: UserContext): Result = {
    try {
      val project = getProject(request.projectId)
      implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
      val dependOnParameterValues = ParamValue.createAll(request.dependsOnParameterValues.getOrElse(Seq.empty),
                                                         autoCompletion.autoCompletionDependsOnParameters, pluginDescription)
      val result = autoCompletion.autoCompletionProvider.autoComplete(request.textQuery.getOrElse(""),
                                                                      dependOnParameterValues,
                                                                      limit = request.workingLimit,
                                                                      offset = request.workingOffset,
                                                                      workspace = workspace)
      Ok(Json.toJson(result.map(_.withNonEmptyLabels).toSeq))
    } catch {
      case ex: IllegalArgumentException =>
        throw BadUserInputException(ex)
    }
  }

  /** Get all item types */
  @Operation(
    summary = "Item type",
    description = "The item types that a user can restrict the search to. The selected type will also influence the available facets.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(SearchApiDoc.itemTypesExample))
        ))
      )
    ))
  def itemTypes(@Parameter(
                  name = "projectId",
                  description = "Optional parameter that fetches the types for a specific project. This will only display types that contain at least one item.",
                  required = false,
                  in = ParameterIn.QUERY,
                  schema = new Schema(implementation = classOf[String])
                )
                projectId: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
    val returnItemTypes = projectId match {
      case Some(_) =>
        ItemType.taskTypes
      case None =>
        ItemType.project +: ItemType.taskTypes
    }
    val results = returnItemTypes.map(itemTypeJson)
    val result = JsObject(Seq(
      "label" -> JsString("Item type"),
      "values" -> JsArray(results)
    ))
    Ok(result)
  }

  private def itemTypeJson(itemType: ItemType): JsValue = {
      JsObject(Seq(
        "id" -> JsString(itemType.id),
        "label" -> JsString(itemType.label)
      ))
  }
}


