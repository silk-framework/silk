package controllers.workflow

import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutor, Workflow}
import play.api.mvc.{Action, Controller}
import plugins.Context

class WorkflowEditorController extends Controller {

  def editor(project: String, task: String) = Action { implicit request =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }

  def report(project: String, task: String) = Action { implicit request =>
    val context = Context.get[Workflow](project, task, request.path)
    val report = context.task.activity[LocalWorkflowExecutor].value
    Ok(views.html.workflow.executionReport(report))
  }
}
