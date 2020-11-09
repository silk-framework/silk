package controllers.transform

import controllers.core.{RequestUserContextAction, UserContextAction}
import javax.inject.Inject
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.{EvaluateTransform => EvaluateTransformTask}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import play.api.mvc.{Action, AnyContent, InjectedController}

/** Endpoints for evaluating transform tasks */
class EvaluateTransform @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController {

  def evaluate(project: String, task: String, ruleName: Option[String], offset: Int, limit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    accessMonitor.saveProjectTaskAccess(project, task)
    Ok(views.html.evaluateTransform.evaluateTransform(context, ruleName.getOrElse("root"), offset, limit))
  }

  def generatedEntities(projectName: String, taskName: String, ruleName: Option[String], offset: Int, limit: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val ruleSchema = ruleName match {
      case Some(name) =>
        task.data.ruleSchemata
            .find(_.transformRule.id.toString == name)
            .getOrElse(throw new NotFoundException(s"Rule $ruleName is not part of task $taskName in project $projectName"))
      case None =>
        task.data.ruleSchemata.head
    }

    // Create execution task
    val evaluateTransform =
      new EvaluateTransformTask(
        source = task.dataSource,
        entitySchema = ruleSchema.inputSchema,
        rules = ruleSchema.transformRule.rules,
        maxEntities = offset + limit
      )
    val entities = evaluateTransform.execute().drop(offset)

    Ok(views.html.evaluateTransform.generatedEntities(entities, project.config.prefixes))
  }

}
