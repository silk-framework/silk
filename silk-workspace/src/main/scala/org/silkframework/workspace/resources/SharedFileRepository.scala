package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.FileResourceManager

import java.io.File

@Plugin(
  id = "file",
  label = "File resources",
  description = "Holds all resources in a specified folder."
)
case class SharedFileRepository(dir: String) extends ResourceRepository with SharedResourceRepository {

  val resourceManager = FileResourceManager(new File(dir))
}
