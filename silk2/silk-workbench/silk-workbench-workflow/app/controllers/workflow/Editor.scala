package controllers.linking

import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object Editor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[LinkingTask](project, task, request.path)
    Ok(views.html.editor.linkingEditor(context))
  }
}