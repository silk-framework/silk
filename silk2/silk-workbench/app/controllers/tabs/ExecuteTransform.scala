package controllers.tabs

import controllers.util.{Stream, Widgets}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import models.CurrentExecuteTransformTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object ExecuteTransform extends Controller {

  def execute(project: String, task: String) = Action { request =>
    val context = Context.get[TransformTask](project, task, request.path)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executeDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.outputModule.tasks.toSeq.map(_.name.toString())

    Ok(views.html.executeTransform.executeTransformDialog(projectName, taskName, outputs))
  }

  def statusStream(project: String, task: String) = Action {
    val stream = Stream.currentTaskStatus(CurrentExecuteTransformTask)
    Ok.chunked(Widgets.taskStatus(stream))
  }

}
