package controllers.transform

import controllers.core.UserContextAction
import javax.inject.Inject
import play.api.mvc.{InjectedController, Action, AnyContent, ControllerComponents}

/** UI dialogs for the transform task */
class TransformDialogs @Inject() () extends InjectedController {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}