package controllers.transform

import controllers.core.UserContextAction
import play.api.mvc.{Action, AnyContent, Controller}

/** UI dialogs for the transform task */
class TransformDialogs extends Controller {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}