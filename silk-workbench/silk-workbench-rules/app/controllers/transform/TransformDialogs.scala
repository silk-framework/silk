package controllers.transform

import controllers.core.UserContextActions
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** UI dialogs for the transform task */
class TransformDialogs @Inject() () extends InjectedController with UserContextActions {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}