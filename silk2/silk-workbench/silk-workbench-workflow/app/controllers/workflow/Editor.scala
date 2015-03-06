package controllers.workflow

import controllers.core.{Stream, Widgets}
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow
import models.CurrentExecutionTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object Editor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val stream = Stream.currentStatus(CurrentExecutionTask)
    Ok.chunked(Widgets.status(stream, "status"))
  }
}