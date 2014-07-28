package controllers.workflow

import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object Editor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[WorkflowTask](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }
}