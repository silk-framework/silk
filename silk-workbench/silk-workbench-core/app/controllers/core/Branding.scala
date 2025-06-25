package controllers.core

import config.WorkbenchConfig
import play.api.mvc.InjectedController
import play.twirl.api.Html

import javax.inject.Inject

class Branding @Inject() () extends InjectedController {

  def logo = Action {
    val bytes = WorkbenchConfig.get.logo.loadAsBytes
    Ok(bytes).as("image/png")
  }

  def logoSmall = Action {
    val bytes = WorkbenchConfig.get.logoSmall.loadAsBytes
    Ok(bytes).as("image/png").withHeaders("Cache-Control" -> "public, max-age=86400")
  }

  def aboutDialog = Action {
    val aboutHtml = Html(WorkbenchConfig.get.about.loadAsString())
    Ok(views.html.aboutDialog(aboutHtml))
  }

  def mdlStyle = Action {
    val bytes = WorkbenchConfig.get.mdlStyle.get.loadAsBytes
    Ok(bytes).as("text/css")
  }

}
