package controllers.core

import config.WorkbenchConfig
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

class Start @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }
}

object Start {

  def deployPath: String = {
    val loggedOutFull = controllers.core.routes.Start.index().url.toString
    val path = loggedOutFull.dropRight("core/start".length)
    if(path.length == 0) {
      "/"
    } else {
      path
    }
  }

}
