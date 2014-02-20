package controllers.tabs

import controllers.util.{Stream, Widgets}
import play.api.mvc.{Controller, Action}
import models._
import models.EvalLink.Correct
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.execution.EvaluateTransform

object EvaluateTransform extends Controller {

  def evaluate(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Create execution task
    val evaluateTransformTask =
      new EvaluateTransform(
        source = project.sourceModule.task(task.dataset.sourceId).source,
        dataset = task.dataset,
        rule = task.rule,
        outputs = Nil
      )

    // Start task in the background
    CurrentEvaluateTransformTask() = evaluateTransformTask


    Ok(views.html.evaluateTransform.evaluateTransform(projectName, taskName))
  }

  def evaluateDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.outputModule.tasks.toSeq.map(_.name.toString())
    Ok(views.html.evaluateTransform.evaluateTransformDialog(projectName, taskName, outputs))
  }

  def statusStream(project: String, task: String) = Action {
    val stream = Stream.currentTaskStatus(CurrentEvaluateTransformTask)
    Ok.chunked(Widgets.taskStatus(stream))
  }

}
