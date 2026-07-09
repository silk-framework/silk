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

  /**
    * Creates a [[TaskResolver]] based on the project ID if available. Throws an exception if no project ID exists.
    */
  def taskResolver: TaskResolver

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None, ExecutionTemplateVariables(GlobalTemplateVariables), TaskResolver.empty)

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables,
            taskResolver: TaskResolver
           ): PlainPluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId, ExecutionTemplateVariables(templateVariables), taskResolver)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id), ExecutionTemplateVariables(project.combinedTemplateVariables), TaskResolver.fromProject(project))
  }

  /**
    * Context for executing a particular task: the execution scope is seeded with the execution
    * variables defined on the task, in addition to the global and project scopes.
    */
  def fromTask(task: Task[_ <: TaskSpec], project: ProjectTrait)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id),
      ExecutionTemplateVariables(Seq(project.combinedTemplateVariables)).withExecutionDefaults(task.executionVariables), TaskResolver.fromProject(project))
  }

  def fromProjectConfig(config: ProjectConfig,
                        projectResource: ResourceManager,
                        templateVariables: TemplateVariablesReader = GlobalTemplateVariables,
                        taskResolver: TaskResolver)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id), ExecutionTemplateVariables(templateVariables), taskResolver)
  }

  def fromReadContext(readContext: ReadContext): PlainPluginContext = {
    PlainPluginContext(readContext.prefixes, readContext.resources, readContext.user, readContext.projectId, readContext.templateVariables, readContext.taskResolver)
  }

  /**
    * Creates an updated plugin context where some parameters are overwritten.
    */
  def updatedPluginContext(context: PluginContext,
                           prefixes: Option[Prefixes] = None): PluginContext = {
    PlainPluginContext(prefixes.getOrElse(context.prefixes), context.resources, context.user, context.projectId, context.templateVariables, context.taskResolver)
  }

  case class PlainPluginContext(prefixes: Prefixes,
                                resources: ResourceManager,
                                user: UserContext,
                                projectId: Option[Identifier],
                                templateVariables: ExecutionTemplateVariables,
                                taskResolver: TaskResolver
                               ) extends PluginContext
}
