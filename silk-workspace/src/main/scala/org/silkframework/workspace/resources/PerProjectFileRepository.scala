package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}

@Plugin(
  id = "projectFile",
  label = "File Resources",
  description = ""
)
case class PerProjectFileRepository(dir: String) extends ResourceRepository {

  val resourceManager = FileResourceManager(new File(dir))

  override def get(project: String): ResourceManager = resourceManager.child(project)
}
