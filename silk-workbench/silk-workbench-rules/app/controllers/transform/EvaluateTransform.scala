package controllers.transform

import org.silkframework.rule.execution.{EvaluateTransform => EvaluateTransformTask}
import org.silkframework.rule.TransformSpec
import org.silkframework.workbench.Context
import org.silkframework.workspace.User
import play.api.mvc.{Action, Controller}
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

class EvaluateTransform extends Controller {

  def evaluate(project: String, task: String, offset: Int, limit: Int) = Action { implicit request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.evaluateTransform.evaluateTransform(context, offset, limit))
  }

  def generatedEntities(projectName: String, taskName: String, offset: Int, limit: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)

    // Create execution task
    val evaluateTransform =
      new EvaluateTransformTask(
        source = task.dataSource,
        dataSelection = task.data.selection,
        rules = task.data.rules,
        maxEntities = offset + limit
      )
    val entities = evaluateTransform.execute().drop(offset)

    Ok(views.html.evaluateTransform.generatedEntities(entities, project.config.prefixes))
  }

}
