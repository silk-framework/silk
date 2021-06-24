package controllers.workflow

import controllers.core.UserContextActions
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class Dialogs @Inject() () extends InjectedController with UserContextActions {

  def workflowTaskDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workflow.workflowTaskDialog(project, ""))
  }

}