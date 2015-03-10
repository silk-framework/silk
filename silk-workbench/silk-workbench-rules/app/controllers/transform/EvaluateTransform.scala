package controllers.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import play.api.mvc.{Controller, Action}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.execution.{EvaluateTransform => EvaluateTransformTask}
import plugins.Context

object EvaluateTransform extends Controller {

  def evaluate(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpecification](project, task, request.path)
    Ok(views.html.evaluateTransform.evaluateTransform(context))
  }

  def generatedEntities(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpecification](taskName)

    // Create execution task
    val evaluateTransform =
      new EvaluateTransformTask(
        source = project.task[Dataset](task.data.selection.datasetId).data,
        dataSelection = task.data.selection,
        rules = task.data.rules
      )

    val entities = evaluateTransform.execute()

    Ok(views.html.evaluateTransform.generatedEntities(entities))
  }

}
