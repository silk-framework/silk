package org.silkframework.workspace.resources

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.FileResourceManager

import java.io.File

@Plugin(
  id = "projectFile",
  label = "Per-project file resources",
  description = "Holds all resources in project specific directories."
)
case class PerProjectFileRepository(dir: String) extends ResourceRepository with PerProjectResourceRepository {

  val resourceManager = FileResourceManager(new File(dir))
}
