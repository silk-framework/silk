package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._

class Workspace
{
  def content(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "workspaceVar" -> "TODO")
  }
}
