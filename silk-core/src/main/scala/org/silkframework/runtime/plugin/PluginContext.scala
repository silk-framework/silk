package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTrait

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

  def empty: PluginContext = PlainPluginContext()

  def apply(prefixes: Prefixes = Prefixes.empty,
            resources: ResourceManager = EmptyResourceManager(),
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None): PluginContext = {
    PlainPluginContext(prefixes, resources, user, projectId)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user, Some(project.id))
  }

  private case class PlainPluginContext(prefixes: Prefixes = Prefixes.empty,
                                        resources: ResourceManager = EmptyResourceManager(),
                                        user: UserContext = UserContext.Empty,
                                        projectId: Option[Identifier] = None) extends PluginContext

}




