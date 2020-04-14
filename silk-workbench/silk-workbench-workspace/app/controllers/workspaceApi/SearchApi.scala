package controllers.workspaceApi

import java.util.logging.Logger

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.workspaceApi.search.ParameterAutoCompletionRequest
import controllers.workspaceApi.search.SearchApiModel._
import javax.inject.Inject
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParameterAutoCompletion, PluginParameterAutoCompletionProvider, PluginRegistry}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * API to search for tasks in the workspace.
  */
class SearchApi @Inject() (implicit accessMonitor: WorkbenchAccessMonitor) extends InjectedController with ControllerUtilsTrait {

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
    val items = accessMonitor.getAccessItems.map { item =>
      JsObject(Seq( // TODO: Add item type, label and main link (e.g. workflow editor, project details page etc.)
        "projectId" -> JsString(item.projectId)
      ) ++ item.taskIdOpt.map(taskId => ("taskId", JsString(taskId))).toSeq)
    }
    Ok(JsArray(items))
  }

  /** Auto-completion service for plugin parameters. */
  def parameterAutoCompletion(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ParameterAutoCompletionRequest] { request =>
      PluginRegistry.pluginDescriptionById(request.pluginId) match {
        case Some(pluginDescription) =>
          pluginDescription.parameters.find(_.name == request.parameterId) match {
            case Some(parameter) =>
              parameter.autoCompletion match {
                case Some(autoCompletion) =>
                  if(hasInvalidDependentParameterValues(request, autoCompletion)) {
                    throw BadUserInputException("No values for depends-on parameters supplied. Values are expected for " +
                        s"following parameters: ${autoCompletion.autoCompletionDependsOnParameters.mkString(", ")}.")
                  } else {
                    val result = autoCompletion.autoCompletionProvider.autoComplete(request.textQuery.getOrElse(""),
                      request.projectId, request.dependsOnParameterValues.getOrElse(Seq.empty),
                      limit = request.workingLimit, offset = request.workingOffset, workspace = workspace)
                    Ok(Json.toJson(result.map(_.withNonEmptyLabels)))
                  }
                case None =>
                  log.warning(s"Parameter '${parameter.name}' of plugin '${request.pluginId}' has no auto-completion support.")
                  NotFound
              }
            case None =>
              log.warning(s"Plugin '${request.pluginId}' does not have a parameter '${request.parameterId}'.")
              NotFound
          }
        case None =>
          log.warning(s"Requesting auto-completion for non-existing plugin: ${request.pluginId}.")
          NotFound
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
      "label" -> JsString("Type"),
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


