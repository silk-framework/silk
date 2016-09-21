package controllers.core

import config.WorkbenchConfig
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html

object Start extends Controller {

  def index = Action { implicit req =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

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
