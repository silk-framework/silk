package controllers.workflow

import controllers.core.{Stream, Widgets}
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask
import models.CurrentExecutionTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object Editor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[WorkflowTask](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val stream = Stream.currentTaskStatus(CurrentExecutionTask)
    Ok.chunked(Widgets.taskStatus(stream, "status"))
  }
}