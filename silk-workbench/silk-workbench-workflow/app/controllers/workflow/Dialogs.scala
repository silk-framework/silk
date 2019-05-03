package controllers.workflow

import controllers.core.UserContextAction
import javax.inject.Inject
import play.api.mvc.{InjectedController, Action, AnyContent, ControllerComponents}

class Dialogs @Inject() () extends InjectedController {

  def workflowTaskDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workflow.workflowTaskDialog(project, ""))
  }

}