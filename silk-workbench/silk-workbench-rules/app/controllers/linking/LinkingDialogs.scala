package controllers.linking

import play.api.mvc.{Action, Controller}

object LinkingDialogs extends Controller {

  def linkingTaskDialog(projectName: String, taskName: String) = Action {
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName))
  }

}