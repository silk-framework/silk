package controllers.transform

import controllers.core.{RequestUserContextAction, UserContextAction}
import javax.inject.Inject
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.{EvaluateTransform => EvaluateTransformTask}
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import play.api.mvc.{Action, AnyContent, ControllerComponents, InjectedController}

/** Endpoints for evaluating transform tasks */
class EvaluateTransform @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController {

  def evaluate(project: String, task: String, offset: Int, limit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    accessMonitor.saveProjectTaskAccess(project, task)
    Ok(views.html.evaluateTransform.evaluateTransform(context, offset, limit))
  }

  def generatedEntities(projectName: String, taskName: String, offset: Int, limit: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
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
