package controllers.linking

import controllers.core.UserContextAction
import play.api.mvc.{Action, AnyContent, Controller}

/** Dialog to configure linking tasks */
class LinkingDialogs extends Controller {

  def linkingTaskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, createDialog))
  }

}