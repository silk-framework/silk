package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "empty",
  label = "Empty resource repository",
  description = "Empty resource repository that does not allow storing any resources."
)
case class EmptyResourceRepository() extends ResourceRepository {

  override def get(project: Identifier): ResourceManager = EmptyResourceManager
}
