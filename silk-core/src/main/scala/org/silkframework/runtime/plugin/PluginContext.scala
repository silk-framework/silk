package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.workspace.ProjectTrait

/**
  * Combines context objects that are available during plugin creation or update.
  */
trait PluginContext {

  def prefixes: Prefixes

  def resources: ResourceManager

  def user: UserContext

}

object PluginContext {

  def empty: PluginContext = PlainPluginContext()

  def apply(prefixes: Prefixes = Prefixes.empty,
            resources: ResourceManager = EmptyResourceManager(),
            user: UserContext = UserContext.Empty): PluginContext = {
    PlainPluginContext(prefixes, resources, user)
  }

  def fromProject(project: ProjectTrait)(implicit user: UserContext): PluginContext = {
    PlainPluginContext(project.config.prefixes, project.resources, user)
  }

  private case class PlainPluginContext(prefixes: Prefixes = Prefixes.empty,
                                        resources: ResourceManager = EmptyResourceManager(),
                                        user: UserContext = UserContext.Empty) extends PluginContext

}




