package controllers.workspaceApi

import controllers.core.RequestUserContextAction
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.search.SearchApiModel._
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * API to search for tasks in the workspace.
  */
class SearchApi @Inject() () extends InjectedController with ControllerUtilsTrait {

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

  /** Get all item types */
  def itemTypes(): Action[AnyContent] = Action {
    val results = ItemType.ordered.map { itemType =>
      JsObject(Seq(
        "id" -> JsString(itemType.id),
        "label" -> JsString(itemType.label)
      ))
    }
    val result = JsObject(Seq(
      "label" -> JsString("Type"),
      "values" -> JsArray(results)
    ))
    Ok(result)
  }
}


