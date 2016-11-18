package org.silkframework.workspace.resources

import java.io.File

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}
import org.silkframework.util.Identifier

@Plugin(
  id = "projectFile",
  label = "File Resources",
  description = "Holds all resources in project specific directories."
)
case class PerProjectFileRepository(dir: String) extends ResourceRepository {

  val resourceManager = FileResourceManager(new File(dir))

  override def get(project: Identifier): ResourceManager = resourceManager.child(project).child("resources")
}
