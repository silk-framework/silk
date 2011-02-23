package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI

class LDEUser extends User {

  override val workspace =  {
    new LDEWorkspace(new URI("http://localhost:8092"))
  }

}