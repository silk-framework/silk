package controllers

import config.WorkbenchConfig
import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.twirl.api.Html

class Workbench @Inject() (assets: Assets, workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {


  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

  def reactUIRoot(): Action[AnyContent] = Action {
    Ok(workspaceReact.indexHtml)
  }

  def reactUI(path: String): Action[AnyContent] = Action {
    Ok(workspaceReact.indexHtml) // Return index.html for everything under the React workspace route
  }
}