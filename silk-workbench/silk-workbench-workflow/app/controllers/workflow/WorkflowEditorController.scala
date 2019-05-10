package controllers.workflow

import java.util.NoSuchElementException

import controllers.core.RequestUserContextAction
import org.silkframework.workbench.Context
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}
import play.api.mvc.{Action, AnyContent, Controller}

/** View endpoints for the workflow editor */
class WorkflowEditorController extends Controller {

  def editor(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.editor.editor(context))
  }

  def report(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    try {
      val report = context.task.activity[LocalWorkflowExecutorGeneratingProvenance].value
      Ok(views.html.workflow.executionReport(report.report, context.project.config.prefixes, context))
    } catch {
      case _: NoSuchElementException =>
        Ok(views.html.clientError("No workflow execution report available. The workflow has probably not been executed, yet."))
    }
  }
}
