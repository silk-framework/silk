package controllers.transform

import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.{EvaluateTransform => EvaluateTransformTask}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** Endpoints for evaluating transform tasks */
class EvaluateTransform @Inject() (implicit accessMonitor: WorkbenchAccessMonitor, workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  def evaluate(project: String, task: String, ruleName: Option[String], offset: Int, limit: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    accessMonitor.saveProjectTaskAccess(project, task)
    Ok(views.html.evaluateTransform.evaluateTransform(context, ruleName.getOrElse("root"), offset, limit))
  }

  def generatedEntities(projectName: String, taskName: String, ruleName: Option[String], offset: Int, limit: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    val task = project.task[TransformSpec](taskName)
    val ruleSchema = ruleName match {
      case Some(name) =>
        val objectMappingId = task.data.objectMappingIdOfRule(name).getOrElse(name)
        task.data.ruleSchemataWithoutEmptyObjectRules
            .find(_.transformRule.id.toString == objectMappingId)
            .getOrElse(throw new NotFoundException(s"Mapping rule '$name' is either an empty object rule, i.e. it has at most a URI rule, or is not part of task '$taskName' in project '$projectName'."))
      case None =>
        task.data.ruleSchemataWithoutEmptyObjectRules.head
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
