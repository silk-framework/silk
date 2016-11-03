package controllers

import config.WorkbenchConfig
import controllers.core.Start
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html

class Workbench extends Controller {

  def index = Action { implicit req =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }

}
