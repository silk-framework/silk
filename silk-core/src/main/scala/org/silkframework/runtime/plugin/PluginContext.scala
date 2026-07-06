package org.silkframework.runtime.plugin

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.templating.{ExecutionTemplateVariables, ExecutionVariablesHolder, GlobalTemplateVariables, TemplateVariableScopes, TemplateVariables, TemplateVariablesReader}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectConfig, ProjectTrait}

/**
  * Combines context objects that are available during plugin creation, update and execution.
  */
trait PluginContext {

  /**
   * The URI namespace prefixes that are defined in the current project.
   */
  def prefixes: Prefixes

  /**
   * The file resources in the current project.
   */
  def resources: ResourceManager

  /**
   * The user that initiated the current operation.
   */
  def user: UserContext

  /**
   * The identifier of the current project.
   */
  def projectId: Option[Identifier]

  /**
   * The template variables that are available in the current scope.
   *
   * The returned value is read-only for the global, project and task scopes, but supports mutation
   * of execution-scope variables via [[ExecutionTemplateVariables.setExecutionVariable]].
   */
  def templateVariables: ExecutionTemplateVariables

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None, ExecutionTemplateVariables(GlobalTemplateVariables))

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables): PlainPluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId, ExecutionTemplateVariables(templateVariables))
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id), ExecutionTemplateVariables(project.combinedTemplateVariables))
  }

  /**
    * Context for executing a particular task: the execution scope is seeded with the execution
    * variables defined on the task, in addition to the global and project scopes.
    */
  def fromTask(task: Task[_ <: TaskSpec], project: ProjectTrait)(implicit user: UserContext): PlainPluginContext = {
    val executionDefaults = TemplateVariables(task.executionVariables.variables.map(_.copy(scope = TemplateVariableScopes.execution)))
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id),
      ExecutionTemplateVariables(Seq(project.combinedTemplateVariables), new ExecutionVariablesHolder(executionDefaults)))
  }

  def fromProjectConfig(config: ProjectConfig,
                        projectResource: ResourceManager,
                        templateVariables: TemplateVariablesReader = GlobalTemplateVariables)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id), ExecutionTemplateVariables(templateVariables))
  }

  def fromReadContext(readContext: ReadContext): PlainPluginContext = {
    PlainPluginContext(readContext.prefixes, readContext.resources, readContext.user, readContext.projectId, readContext.templateVariables)
  }

  /**
   * Creates an updated plugin context where some parameters are overwritten.
   */
  def updatedPluginContext(context: PluginContext,
                           prefixes: Option[Prefixes] = None): PluginContext = {
    PlainPluginContext(prefixes.getOrElse(context.prefixes), context.resources, context.user, context.projectId, context.templateVariables)
  }

  case class PlainPluginContext(prefixes: Prefixes,
                                resources: ResourceManager,
                                user: UserContext,
                                projectId: Option[Identifier],
                                templateVariables: ExecutionTemplateVariables) extends PluginContext

}
