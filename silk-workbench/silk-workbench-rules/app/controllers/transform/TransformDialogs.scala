package controllers.transform

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import play.api.mvc.{Action, AnyContent, Controller}

/** UI dialogs for the transform task */
class TransformDialogs extends Controller {

  def transformationTaskDialog(projectName: String, taskName: String, createDialog: Boolean = false): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, createDialog))
  }

  def deleteRuleDialog(ruleName: String): Action[AnyContent] = Action {
    Ok(views.html.dialogs.deleteRuleDialog(ruleName))
  }

}