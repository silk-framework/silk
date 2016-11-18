package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "file",
  label = "File resources",
  description = "Holds all resources in a specified folder."
)
case class FileRepository(dir: String) extends ResourceRepository {

  val resourceManager = FileResourceManager(new File(dir))

  override def get(project: Identifier): ResourceManager = resourceManager
}
