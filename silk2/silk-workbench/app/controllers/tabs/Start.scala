package controllers.tabs

import play.api.mvc.{Action, Controller}
import models.WorkbenchConfig
import play.twirl.api.Html

object Start extends Controller {

  def index = Action {
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }
}
