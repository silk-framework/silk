package org.silkframework.workspace.xml

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.FileResourceManager

@Plugin(
  id = "file",
  label = "Filesystem",
  description = "Workspace on filesystem"
)
case class FileWorkspaceProvider(dir: String) extends XmlWorkspaceProvider(new FileResourceManager(dir))