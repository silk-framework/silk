package controllers.workspaceApi

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.search.SearchApiModel._
import javax.inject.Inject
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * API to search for tasks in the workspace.
  */
class SearchApi @Inject() (implicit accessMonitor: WorkbenchAccessMonitor) extends InjectedController with ControllerUtilsTrait {

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
      JsObject(Seq(
        "projectId" -> JsString(item.projectId)
      ) ++ item.taskIdOpt.map(taskId => ("taskId", JsString(taskId))).toSeq)
    }
    Ok(JsArray(items))
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


