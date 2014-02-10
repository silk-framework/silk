package controllers.tabs

import controllers.util.{Stream, Widgets}
import play.api.mvc.{Controller, Action}
import models.{CurrentExecuteTransformTask, LinkSorter, CurrentGenerateLinksTask, EvalLink}
import models.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workspace.User

object ExecuteTransform extends Controller {

  def execute(projectName: String, taskName: String) = Action {
    Ok(views.html.executeTransform.executeTransform(projectName, taskName))
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
