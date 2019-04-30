package controllers.workflow

import controllers.core.UserContextAction
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class Dialogs @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def workflowTaskDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workflow.workflowTaskDialog(project, ""))
  }

}