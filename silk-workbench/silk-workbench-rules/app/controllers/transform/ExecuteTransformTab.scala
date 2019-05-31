package controllers.transform

import controllers.core.{RequestUserContextAction, Stream, Widgets}
import javax.inject.Inject
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.http.ContentTypes
import play.api.mvc.{InjectedController, Action, AnyContent, ControllerComponents}

/** Endpoints for the 'Execute' page of a transform task */
class ExecuteTransformTab @Inject() () extends InjectedController {

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
    Ok.chunked(Widgets.statusStream(stream)).as(ContentTypes.HTML)
  }

  def updateReportStream(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val stream = Stream.status(task.activity[ExecuteTransform].control.status, _.succeeded)
    Ok.chunked(Widgets.autoReload("reload", stream))
  }

}
