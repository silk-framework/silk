package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
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

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext(Prefixes.empty, EmptyResourceManager(), UserContext.Empty, None)

  def apply(prefixes: Prefixes,
            resources: ResourceManager,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None): PluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id))
  }

  def fromProjectConfig(config: ProjectConfig, projectResource: ResourceManager)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(config.prefixes, projectResource, user, Some(config.id))
  }

  def fromReadContext(readContext: ReadContext): PluginContext = {
    PlainPluginContext(readContext.prefixes, readContext.resources, readContext.user, readContext.projectId)
  }

  private case class PlainPluginContext(prefixes: Prefixes,
                                        resources: ResourceManager,
                                        user: UserContext,
                                        projectId: Option[Identifier]) extends PluginContext

}




