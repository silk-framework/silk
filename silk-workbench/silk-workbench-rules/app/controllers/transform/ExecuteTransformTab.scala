package controllers.transform

import controllers.core.{RequestUserContextAction, Stream, Widgets}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.mvc.{Action, AnyContent, Controller}

/** Endpoints for the 'Execute' page of a transform task */
class ExecuteTransformTab extends Controller {

  def execute(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executionReport(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request =>implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val report = context.task.activity[ExecuteTransform].value
    Ok(views.html.executeTransform.transformReport(context.task))
  }

  def statusStream(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val stream = Stream.status(task.activity[ExecuteTransform].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
