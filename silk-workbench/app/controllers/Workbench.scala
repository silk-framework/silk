package controllers

import config.WorkbenchConfig
import controllers.core.RequestUserContextAction
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

class Workbench @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

}
