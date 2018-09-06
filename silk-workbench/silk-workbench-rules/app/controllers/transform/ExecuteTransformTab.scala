package controllers.transform

import controllers.core.{Stream, Widgets}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.TransformSpec
import org.silkframework.workbench.Context
import org.silkframework.workspace.User
import play.api.mvc.{Action, Controller}

class ExecuteTransformTab extends Controller {

  def execute(project: String, task: String) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executeStatistics(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    val report = context.task.activity[ExecuteTransform].value
    Ok(views.html.executeTransform.transformStatistics(context.task, report, context.project.config.prefixes))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val stream = Stream.status(task.activity[ExecuteTransform].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
