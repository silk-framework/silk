package controllers.tabs

import play.api.mvc.{Action, Controller}
import play.api.templates.Html
import models.WorkbenchConfig
import scala.io.Source

object Start extends Controller {

  def index = Action {
    val welcome = Html(Source.fromFile(WorkbenchConfig.get.welcome).getLines.mkString("\n"))

    Ok(views.html.start(welcome))
  }
}
