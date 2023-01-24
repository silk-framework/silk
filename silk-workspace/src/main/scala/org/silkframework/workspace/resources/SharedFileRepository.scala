package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager, UrlResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "file",
  label = "File resources",
  description = "Holds all resources in a specified folder."
)
case class SharedFileRepository(dir: String) extends ResourceRepository with SharedResourceRepository {

  val resourceManager = UrlResourceManager(FileResourceManager(new File(dir)))
}
