package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.FileResourceManager

@Plugin(
  id = "file",
  label = "Filesystem",
  description = "Workspace on filesystem"
)
case class FileWorkspaceProvider(dir: String) extends XmlWorkspaceProvider(new FileResourceManager(dir))