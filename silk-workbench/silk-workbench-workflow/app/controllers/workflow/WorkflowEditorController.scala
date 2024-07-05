package controllers.workflow

import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import models.workflow.WorkflowConfig
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** View endpoints for the workflow editor */
class WorkflowEditorController @Inject() (implicit accessMonitor: WorkbenchAccessMonitor, workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  def activityControl(projectId: String, taskId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectId)
    val task = project.anyTask(taskId)
    val activity = task.activity(WorkflowConfig.executorName)
    Ok(views.html.workflow.workflowControl(activity, showButtons = true, insideIFrame = true))
  }

  def reports(project: String, task: String): Action[AnyContent] = reportImpl(project, task, None)

  def report(project: String, task: String, report: String): Action[AnyContent] = reportImpl(project, task, Some(report))

  def workflowNodeReport(project: String, task: String, nodeId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.workflowNodeExecutionReport(context, nodeId))
  }

  private def reportImpl(project: String, task: String, report: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[Workflow](project, task, request.path)
    Ok(views.html.workflow.executionReport(context, report))
  }
}
