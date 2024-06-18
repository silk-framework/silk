package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariablesReader}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectConfig, ProjectTrait}

/**
  * Combines context objects that are available during plugin creation or update.
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
   */
  def templateVariables: TemplateVariablesReader

  /**
   * Set if this task or plugin is opened or executed in the context of a workflow.
   */
  def workflowId: Option[Identifier]

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None, GlobalTemplateVariables, None)

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables,
            workflowId: Option[Identifier] = None): PlainPluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId, templateVariables, workflowId)
  }

  def fromProject(project: ProjectTrait, workflowId: Option[Identifier] = None)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id), project.combinedTemplateVariables, workflowId)
  }

  def fromProjectConfig(config: ProjectConfig,
                        projectResource: ResourceManager,
                        templateVariables: TemplateVariablesReader = GlobalTemplateVariables,
                        workflowId: Option[Identifier] = None)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id), templateVariables, workflowId)
  }

  def fromReadContext(readContext: ReadContext): PlainPluginContext = {
    PlainPluginContext(readContext.prefixes, readContext.resources, readContext.user, readContext.projectId, readContext.templateVariables, readContext.workflowId)
  }

  case class PlainPluginContext(prefixes: Prefixes,
                                resources: ResourceManager,
                                user: UserContext,
                                projectId: Option[Identifier],
                                templateVariables: TemplateVariablesReader,
                                workflowId: Option[Identifier]) extends PluginContext

}




