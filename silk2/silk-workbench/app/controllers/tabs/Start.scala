package controllers.tabs

import play.api.mvc.{Action, Controller}
import play.api.templates.Html
import models.WorkbenchConfig

object Start extends Controller {

  def index = Action {
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString)
    Ok(views.html.start(welcome))
  }
}
