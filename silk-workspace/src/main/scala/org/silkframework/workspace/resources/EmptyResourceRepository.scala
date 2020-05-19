package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "empty",
  label = "Empty resource repository",
  description = "Empty resource repository that does not allow storing any resources."
)
case class EmptyResourceRepository() extends ResourceRepository {

  override def sharedResources: Boolean = false

  override def get(project: Identifier): ResourceManager = EmptyResourceManager()

  override def removeProjectResources(project: Identifier): Unit = {}
}
