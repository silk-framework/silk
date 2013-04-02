package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action

object Editor extends Controller {

  def editor(project: String, task: String) = Action {
    Ok(views.html.editor.editor(project, task))
  }

}
