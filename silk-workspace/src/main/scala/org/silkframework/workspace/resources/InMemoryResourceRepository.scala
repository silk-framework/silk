package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "inMemoryResourceRepository",
  label = "In-memory resources",
  description = "Holds all resource in-memory."
)
case class InMemoryResourceRepository() extends ResourceRepository with PerProjectResourceRepository {

  val resourceManager = InMemoryResourceManager()
}
