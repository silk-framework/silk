package controllers.transform

import play.api.mvc.{Action, AnyContent, Controller}

class TransformDialogs extends Controller {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = Action {
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}