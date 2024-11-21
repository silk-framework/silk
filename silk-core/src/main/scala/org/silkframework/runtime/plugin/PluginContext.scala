package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariablesReader}
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
   */
  def templateVariables: TemplateVariablesReader

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None, GlobalTemplateVariables)

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables): PlainPluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId, templateVariables)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id), project.combinedTemplateVariables)
  }

  def fromProjectConfig(config: ProjectConfig,
                        projectResource: ResourceManager,
                        templateVariables: TemplateVariablesReader = GlobalTemplateVariables)(implicit user: UserContext): PlainPluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id), templateVariables)
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
                                templateVariables: TemplateVariablesReader) extends PluginContext

}




