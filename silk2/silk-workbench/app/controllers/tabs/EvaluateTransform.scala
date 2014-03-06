package controllers.tabs

import play.api.mvc.{Controller, Action}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.execution.{EvaluateTransform => EvaluateTransformTask}
import scala.concurrent.ExecutionContext.Implicits.global

object EvaluateTransform extends Controller {

  def evaluate(projectName: String, taskName: String) = Action {
    Ok(views.html.evaluateTransform.evaluateTransform(projectName, taskName))
  }

  def generatedEntities(projectName: String, taskName: String) = Action.async {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Create execution task
    val evaluateTransform =
      new EvaluateTransformTask(
        source = project.sourceModule.task(task.dataset.sourceId).source,
        dataset = task.dataset,
        rules = Seq(task.rule)
      )

    for(entities <- evaluateTransform.runInBackground()) yield {
      Ok(views.html.evaluateTransform.generatedEntities(entities))
    }
  }

}
