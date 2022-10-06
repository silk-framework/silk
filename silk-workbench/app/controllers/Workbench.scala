package controllers

import config.WorkbenchConfig.WorkspaceReact
import config.baseUrl
import controllers.core.UserContextActions
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class Workbench @Inject() (implicit workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  def index: Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    Redirect(s"$baseUrl/workbench?itemType=project")
  }

  def reactUIRoot(): Action[AnyContent] = Action {
    Ok(workspaceReact.indexHtml)
  }

  def reactUI(path: String): Action[AnyContent] = Action {
    Ok(workspaceReact.indexHtml) // Return index.html for everything under the React workspace route
  }
}