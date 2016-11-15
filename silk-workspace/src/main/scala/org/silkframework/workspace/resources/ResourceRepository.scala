package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

trait ResourceRepository {

  def get(project: Identifier): ResourceManager

}

object ResourceRepository {

  private lazy val instance = PluginRegistry.createFromConfig("workspace.repository")

  def apply(): ResourceRepository = instance

}
