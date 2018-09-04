package controllers

import config.WorkbenchConfig
import controllers.core.RequestUserContextAction
import play.api.mvc.{Action, AnyContent, Controller}
import play.twirl.api.Html

class Workbench extends Controller {

  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

}
