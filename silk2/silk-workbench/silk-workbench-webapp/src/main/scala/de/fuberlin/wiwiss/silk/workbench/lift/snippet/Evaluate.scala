package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.lift.util.Widgets
import de.fuberlin.wiwiss.silk.workbench.workspace.User

class Evaluate
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "control" -> Widgets.taskControl(User().evaluationTask, cancelable = true))
  }
}
