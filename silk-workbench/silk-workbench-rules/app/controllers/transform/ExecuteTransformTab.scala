package controllers.transform

import controllers.core.{RequestUserContextAction, Stream, Widgets}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.mvc.{Action, AnyContent, Controller}

/** Endpoints for the 'Execute' page of a transform task */
class ExecuteTransformTab extends Controller {

  def execute(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executeStatistics(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val report = context.task.activity[ExecuteTransform].value
    Ok(views.html.executeTransform.transformStatistics(context.task, report, context.project.config.prefixes))
  }

  def executeDialog(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val outputs = project.tasks[GenericDatasetSpec].toSeq.map(_.id.toString())
    Ok(views.html.executeTransform.executeTransformDialog(projectName, taskName, outputs))
  }

  def statusStream(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val stream = Stream.status(task.activity[ExecuteTransform].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
