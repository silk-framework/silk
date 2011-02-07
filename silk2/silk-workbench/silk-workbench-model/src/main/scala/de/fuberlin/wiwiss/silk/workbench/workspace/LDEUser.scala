package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI

class LDEUser extends User {

  override def workspace =
 {
    val workspaceUri = new URI("http://localhost:11/")
    new LDEWorkspace(workspaceUri)
  }

}