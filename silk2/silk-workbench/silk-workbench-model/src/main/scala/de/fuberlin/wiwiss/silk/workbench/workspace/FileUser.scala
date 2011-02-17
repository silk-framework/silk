package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File

class FileUser extends User
{
  override val workspace =
  {
    val workspaceFile = new File("./workspace/")
    workspaceFile.mkdirs()
    new FileWorkspace(workspaceFile)
  }
}

