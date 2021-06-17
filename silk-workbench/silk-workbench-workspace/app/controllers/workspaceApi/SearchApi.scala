package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.search.SearchApiModel._
import controllers.workspaceApi.search.{ItemType, ParameterAutoCompletionRequest}
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.Dataset
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController, Result}

import java.util.logging.Logger
import javax.inject.Inject

/**
  * API to search for tasks in the workspace.
  */
class SearchApi @Inject() (implicit accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)
  implicit val autoCompletionResultJsonFormat: Format[AutoCompletionResult] = Json.format[AutoCompletionResult]

  /** Search tasks by text search query. */
  def search(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[SearchRequest] { searchRequest =>
      Ok(searchRequest())
    }
  }

  /** Faceted search API for the workspace search */
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
  def itemTypes(projectId: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
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


