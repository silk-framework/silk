package controllers.linking

import play.api.mvc.{Action, AnyContent, Controller}

class LinkingDialogs extends Controller {

  def linkingTaskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = Action {
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, createDialog))
  }

}