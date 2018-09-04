package controllers.core

import config.WorkbenchConfig
import play.api.mvc.{Action, AnyContent, Controller}
import play.twirl.api.Html

class Start extends Controller {

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
