package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.search.SearchApiModel._
import controllers.workspaceApi.search.{ItemType, ParameterAutoCompletionRequest}
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.Dataset
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParameterAutoCompletion, PluginDescription, PluginObjectParameter, PluginRegistry}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController, Result}

import java.util.logging.Logger
import javax.inject.Inject

/**
  * API to search for tasks in the workspace.
  */
@Tag(name = "Search")
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
          mediaType = "application/json"
        )
      )
    )
  ))
  @RequestBody(description = "Search request", required = true,
    content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[SearchRequest]))))
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
          schema = new Schema(implementation = classOf[FacetedSearchResult]))
        )
      )
    )
  )
  @RequestBody(
    description =
                """If the optional project parameter is defined, only artifacts from that project are fetched.
                If the 'itemType' parameter is defined, then only artifacts of this type are fetched.
                Valid values are: Project, Dataset, Transformation, Linking, Workflow, Task.
                The 'textQuery' parameter is a conjunctive multi word query. The single words can be scattered over
                different artifact properties, e.g. one in label and one in description.
                The 'offset' and 'limit' parameters allow for paging through the result list.
                The limit will default to 10 if it is not provided. It can be disabled by setting it to '0', which will return all results.
                The optional sort parameter allows for sorting the result list by a specific artifact property, e.g. label, creation date, update date.
                The 'facets' parameter defines what facets are set to which values. The 'keyword' facet allows multiple values to be set.
                """,
    content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[FacetedSearchRequest]))))
  def facetedSearch(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[FacetedSearchRequest] { facetedSearchRequest =>
      Ok(facetedSearchRequest())
    }
  }

  /** Recently viewed items of user. */
  def recentlyViewedItems(): Action[AnyContent] = UserContextAction { implicit userContext =>
    val w = workspace
    val accessedItems = accessMonitor.getAccessItems.reverse
    val projects = accessedItems.map(_.projectId).distinct.flatMap(projectId => w.findProject(projectId))
    val availableProjects = projects.map(p => (p.name.toString, p)).toMap
    val availableItems = accessedItems.filter(item => availableProjects.contains(item.projectId))
    val items = availableItems flatMap { item =>
      val project = availableProjects(item.projectId)
      val taskOpt = item.taskIdOpt.flatMap(taskId => project.anyTaskOption(taskId))
      if(item.taskIdOpt.isDefined && taskOpt.isEmpty) {
        None
      } else {
        val itemType = if(taskOpt.isEmpty) ItemType.project else ItemType.itemType(taskOpt.get.data)
        val taskData = for (task <- taskOpt) yield {
          val pd = PluginDescription(task)
          Seq(
            "taskId" -> JsString(task.id),
            "taskLabel" -> JsString(taskOpt.get.metaData.label),
            PLUGIN_ID -> JsString(pd.id),
            PLUGIN_LABEL -> JsString(pd.label)
          )
        }
        Some(JsObject(Seq(
          "projectId" -> JsString(item.projectId),
          "projectLabel" -> JsString(project.config.metaData.label),
          "itemType" -> JsString(itemType.id),
          "itemLinks" -> Json.toJson(ItemType.itemTypeLinks(
            itemType,
            project.name,
            taskOpt.map(_.id.toString).getOrElse(project.name.toString),
            taskOpt.map(_.data)
          ))
        ) ++ taskData.toSeq.flatten))
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
          mediaType = "application/json"
        )
        )
      )
    ))
  @RequestBody(
    description =
      """The 'pluginId' and 'parameterId' reference the parameter of a plugin, they values can be read e.g. from the /plugins endpoint.
        The 'projectId' provides the project context for parameters that hold values that are project specific, e.g. task references.
        The 'dependsOnParameterValues' parameter contains all the values of other parameters this auto-completion depends on. E.g. if a
        plugin has a parameter 'project' and 'projectTask', the 'projectTask' parameter may depend on 'project',
        because only when the project is known then the auto-completion of project tasks can be peformed. The list
        of parameters are returned in the plugin parameter description.
        The 'textQuery' parameter is a conjunctive multi word query matching against the possible results.
        The 'offset' and 'limit' parameters allow for paging through the result list.
        """,
    required = true,
    content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[ParameterAutoCompletionRequest]))))
  def parameterAutoCompletion(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ParameterAutoCompletionRequest] { request =>
      PluginRegistry.pluginDescriptionsById(
        request.pluginId,
        // Plugin ID collisions exist, we need to filter the types of plugins.
        assignableTo = Some(Seq(classOf[TaskSpec], classOf[PluginObjectParameter], classOf[Transformer], classOf[Dataset]))
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
    pluginDescription.parameters.find(_.name == request.parameterId) match {
      case Some(parameter) =>
        parameter.autoCompletion match {
          case Some(autoCompletion) =>
            parameterAutoCompletion(request, autoCompletion)
          case None =>
            log.warning(s"Parameter '${parameter.name}' of plugin '${request.pluginId}' has no auto-completion support.")
            NotFound
        }
      case None =>
        log.warning(s"Plugin '${request.pluginId}' does not have a parameter '${request.parameterId}'.")
        NotFound
    }
  }

  private def parameterAutoCompletion(request: ParameterAutoCompletionRequest,
                                      autoCompletion: ParameterAutoCompletion)
                                     (implicit userContext: UserContext): Result = {
    if (hasInvalidDependentParameterValues(request, autoCompletion)) {
      throw BadUserInputException("No values for depends-on parameters supplied. Values are expected for " +
          s"following parameters: ${autoCompletion.autoCompletionDependsOnParameters.mkString(", ")}.")
    } else {
      try {
        val result = autoCompletion.autoCompletionProvider.autoComplete(request.textQuery.getOrElse(""),
          request.projectId, request.dependsOnParameterValues.getOrElse(Seq.empty),
          limit = request.workingLimit, offset = request.workingOffset, workspace = workspace)
        Ok(Json.toJson(result.map(_.withNonEmptyLabels).toSeq))
      } catch {
        case ex: IllegalArgumentException =>
          throw BadUserInputException(ex)
      }
    }
  }

  private def hasInvalidDependentParameterValues(request: ParameterAutoCompletionRequest, autoCompletion: ParameterAutoCompletion): Boolean = {
    autoCompletion.autoCompletionDependsOnParameters.nonEmpty &&
      autoCompletion.autoCompletionDependsOnParameters.size != request.dependsOnParameterValues.map(_.size).getOrElse(0)
  }

  /** Get all item types */
  @Operation(
    summary = "Item type",
    description = "The item types that a user can restrict the search to. The selected type will also influence the available facets.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def itemTypes(@Parameter(description = "Optional parameter that fetches the types for a specific project. This will only display types that contain at least one item.")
                projectId: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
    val returnItemTypes = projectId match {
      case Some(_) =>
        ItemType.ordered.filterNot(_ == ItemType.project)
      case None =>
        ItemType.ordered
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


