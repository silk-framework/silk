package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.{IOException, File}
import java.util.logging.{Level, Logger}

class FileUser extends User {
  override def workspace = FileUser.workspace
}

object FileUser {
  private lazy val workspaceDir = new File(System.getProperty("user.home") + "/.silk/workspace/")

  val workspace = {
    try {
      if(!workspaceDir.exists && !workspaceDir.mkdirs()) throw new IOException("Could not create workspace directory at: " + workspaceDir.getCanonicalPath)

      new FileWorkspace(workspaceDir)
    }
    catch {
      case ex: Exception => {
        Logger.getLogger(FileUser.getClass.getName).log(Level.SEVERE, "Error loading workspace", ex)
        throw ex
      }
    }
  }
}

