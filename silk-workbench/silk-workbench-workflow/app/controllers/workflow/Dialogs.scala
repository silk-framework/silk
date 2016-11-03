package controllers.workflow

import play.api.mvc.{Action, Controller}

class Dialogs extends Controller {

  def workflowTaskDialog(project: String) = Action {
    Ok(views.html.workflow.workflowTaskDialog(project, ""))
  }

}