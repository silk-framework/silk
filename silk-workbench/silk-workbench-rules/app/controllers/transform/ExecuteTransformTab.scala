package controllers.transform

import controllers.core.{Stream, Widgets}
import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.execution.ExecuteTransform
import org.silkframework.workspace.User
import play.api.mvc.{Action, Controller}
import plugins.Context

object ExecuteTransformTab extends Controller {

  def execute(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpecification](project, task, request.path)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executeStatistics(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpecification](project, task, request.path)
    val status = context.task.activity[ExecuteTransform].value
    Ok(views.html.executeTransform.transformStatistics(context, status))
  }

  def executeDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.tasks[Dataset].toSeq.map(_.name.toString())
    Ok(views.html.executeTransform.executeTransformDialog(projectName, taskName, outputs))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpecification](taskName)
    val stream = Stream.status(task.activity[ExecuteTransform].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
