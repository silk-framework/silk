package controllers.linking

import controllers.core.UserContextAction
import javax.inject.Inject
import play.api.mvc.{InjectedController, Action, AnyContent, ControllerComponents}

/** Dialog to configure linking tasks */
class LinkingDialogs @Inject() () extends InjectedController {

  def linkingTaskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, createDialog))
  }

}