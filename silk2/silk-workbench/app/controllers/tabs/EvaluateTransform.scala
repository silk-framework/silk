package controllers.tabs

import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import play.api.mvc.{Controller, Action}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.execution.{EvaluateTransform => EvaluateTransformTask}
import plugins.Context
import scala.concurrent.ExecutionContext.Implicits.global

object EvaluateTransform extends Controller {

  def evaluate(project: String, task: String) = Action { request =>
    val context = Context.get[TransformTask](project, task, request.path)
    Ok(views.html.evaluateTransform.evaluateTransform(context))
  }

  def generatedEntities(projectName: String, taskName: String) = Action.async {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)

    // Create execution task
    val evaluateTransform =
      new EvaluateTransformTask(
        source = project.sourceModule.task(task.dataset.sourceId).source,
        dataset = task.dataset,
        rules = task.rules
      )

    for(entities <- evaluateTransform.runInBackground()) yield {
      Ok(views.html.evaluateTransform.generatedEntities(entities))
    }
  }

}
