package org.silkframework.workspace.xml

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, UrlResourceManager}

@Plugin(
  id = "file",
  label = "Workspace on filesystem",
  description = "Holds the workspace in a specified directory on the filesystem."
)
case class FileWorkspaceProvider(dir: String) extends XmlWorkspaceProvider(UrlResourceManager(new FileResourceManager(dir)))