package controllers.workspaceApi

import controllers.core.UserContextAction
import controllers.core.util.ControllerUtilsTrait
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * API endpoints for initialization of the frontend application.
  */
case class InitApi @Inject()() extends InjectedController with ControllerUtilsTrait {
  def init(): Action[AnyContent] = UserContextAction { implicit userContext =>
    val emptyWorkspace = workspace.projects.isEmpty
    Ok(Json.obj(
      "emptyWorkspace" -> emptyWorkspace
    ))
  }
}
