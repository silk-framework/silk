package org.silkframework.workspace.xml

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.FileResourceManager

@Plugin(
  id = "fileWorkspaceProvider",
  label = "Workspace on filesystem",
  description = "Holds the workspace in a specified directory on the filesystem."
)
case class FileWorkspaceProvider(dir: String) extends XmlWorkspaceProvider(new FileResourceManager(dir))