package controllers.workflow

import controllers.core.{Stream, Widgets}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutor}
import play.api.mvc.{Action, Controller}
import plugins.Context

object Editor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }

  def statusStream(projectName: String, taskName: String) = Action { request =>
    val context = Context.get[Workflow](projectName, taskName, request.path)
    val activity = context.task.activity[WorkflowExecutor].control
    val stream = Stream.status(activity.status)
    Ok.chunked(Widgets.statusStream(stream, "status"))
  }
}