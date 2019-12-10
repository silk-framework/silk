package controllers

import config.WorkbenchConfig
import controllers.core.RequestUserContextAction
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.twirl.api.Html

class Workbench @Inject() (assets: Assets) extends InjectedController {


  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

  def reactUI(): Action[AnyContent] = Action {
    Ok(WorkbenchConfig.indexHtml)
  }
}