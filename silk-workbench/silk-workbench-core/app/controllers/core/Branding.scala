package controllers.core

import config.WorkbenchConfig
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html

object Branding extends Controller {

  def logo = Action {
    val imgStream = WorkbenchConfig.get.logo.load
    val bytes = scala.Stream.continually(imgStream.read).takeWhile(_ != -1).map(_.toByte).toArray
    Ok(bytes).as("image/png")
  }

  def aboutDialog = Action {
    val aboutHtml = Html(WorkbenchConfig.get.about.loadAsString)
    Ok(views.html.aboutDialog(aboutHtml))
  }

  def mdlStyle = Action {
    val bytes = WorkbenchConfig.get.mdlStyle.get.loadAsBytes
    Ok(bytes).as("text/css")
  }

}
