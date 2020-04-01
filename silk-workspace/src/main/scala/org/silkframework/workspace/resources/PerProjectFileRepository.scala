package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager, UrlResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "projectFile",
  label = "Per-project file resources",
  description = "Holds all resources in project specific directories."
)
case class PerProjectFileRepository(dir: String) extends ResourceRepository with PerProjectResourceRepository {

  val resourceManager = UrlResourceManager(FileResourceManager(new File(dir)))
}
