package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, InMemoryResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "inMemory",
  label = "In-Memory Resources",
  description = ""
)
case class InMemoryResourceRepository() extends ResourceRepository {

  val resourceManager = InMemoryResourceManager()

  override def get(project: Identifier): ResourceManager = resourceManager.child(project)
}
