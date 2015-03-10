package controllers.linking

import play.api.mvc.{Action, Controller}

object LinkingDialogs extends Controller {

  def linkingTaskDialog(project: String, task: String) = Action {
    Ok(views.html.dialogs.linkingTaskDialog(project, task))
  }

}