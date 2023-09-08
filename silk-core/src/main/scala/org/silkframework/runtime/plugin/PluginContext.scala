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

  def prefixes: Prefixes

  def resources: ResourceManager

  def user: UserContext

  def projectId: Option[Identifier]

  def templateVariables: TemplateVariablesReader

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None, GlobalTemplateVariables)

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables): PluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId, templateVariables)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id), project.combinedTemplateVariables)
  }

  def fromProjectConfig(config: ProjectConfig,
                        projectResource: ResourceManager,
                        templateVariables: TemplateVariablesReader = GlobalTemplateVariables)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id), templateVariables)
  }

  def fromReadContext(readContext: ReadContext): PluginContext = {
    PlainPluginContext(readContext.prefixes, readContext.resources, readContext.user, readContext.projectId, readContext.templateVariables)
  }

  private case class PlainPluginContext(prefixes: Prefixes,
                                        resources: ResourceManager,
                                        user: UserContext,
                                        projectId: Option[Identifier],
                                        templateVariables: TemplateVariablesReader) extends PluginContext

}




