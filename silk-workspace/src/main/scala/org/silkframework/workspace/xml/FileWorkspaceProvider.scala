package org.silkframework.workspace.xml

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{FileResourceManager, UrlResourceManager}

@Plugin(
  id = "file",
  label = "Filesystem",
  description = "Workspace on filesystem"
)
case class FileWorkspaceProvider(dir: String) extends XmlWorkspaceProvider(UrlResourceManager(new FileResourceManager(dir)))