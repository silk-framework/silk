package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.{IOException, File}

class FileUser extends User
{
  override def workspace = FileUser.workspace
}

object FileUser
{
  private val workspaceDir = new File(System.getProperty("user.home") + "/.silk/workspace/")

  val workspace =
  {
    if(!workspaceDir.mkdirs()) throw new IOException("Could not create workspace directory at: " + workspaceDir.getCanonicalPath)

    new FileWorkspace(workspaceDir)
  }
}

