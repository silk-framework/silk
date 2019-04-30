package controllers.transform

import controllers.core.UserContextAction
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

/** UI dialogs for the transform task */
class TransformDialogs @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}