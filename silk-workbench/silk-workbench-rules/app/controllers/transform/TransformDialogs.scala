package controllers.transform

import play.api.mvc.{Action, Controller}

object TransformDialogs extends Controller {

  def transformationTaskDialog(project: String, task: String) = Action {
    Ok(views.html.dialogs.transformationTaskDialog(project, task))
  }

}