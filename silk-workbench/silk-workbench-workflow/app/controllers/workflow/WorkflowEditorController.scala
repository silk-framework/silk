package controllers.workflow

import controllers.core.UserContextActions
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** View endpoints for the workflow editor */
class WorkflowEditorController @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions {

  def editor(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    accessMonitor.saveProjectTaskAccess(project, task)
    Ok(views.html.workflow.editor.editor(context))
  }

  def reports(project: String, task: String): Action[AnyContent] = reportImpl(project, task, None)

  def report(project: String, task: String, report: String): Action[AnyContent] = reportImpl(project, task, Some(report))

  private def reportImpl(project: String, task: String, report: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.executionReport(context, report))
  }
}
