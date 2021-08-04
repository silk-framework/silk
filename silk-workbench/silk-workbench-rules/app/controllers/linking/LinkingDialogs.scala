package controllers.linking

import controllers.core.UserContextActions
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** Dialog to configure linking tasks */
class LinkingDialogs @Inject() () extends InjectedController with UserContextActions {

  def linkingTaskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, createDialog))
  }

}