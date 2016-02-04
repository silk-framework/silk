package controllers.transform

import play.api.mvc.{Action, Controller}

object TransformDialogs extends Controller {

  def transformationTaskDialog(projectName: String, taskName: String) = Action {
    Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName))
  }

}