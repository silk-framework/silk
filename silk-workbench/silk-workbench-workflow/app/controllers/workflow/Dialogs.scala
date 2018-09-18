package controllers.workflow

import controllers.core.UserContextAction
import play.api.mvc.{Action, AnyContent, Controller}

class Dialogs extends Controller {

  def workflowTaskDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workflow.workflowTaskDialog(project, ""))
  }

}