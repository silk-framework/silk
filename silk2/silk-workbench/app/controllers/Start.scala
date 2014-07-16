package controllers

import config.WorkbenchConfig
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html

object Start extends Controller {

  def index = Action {
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }
}
