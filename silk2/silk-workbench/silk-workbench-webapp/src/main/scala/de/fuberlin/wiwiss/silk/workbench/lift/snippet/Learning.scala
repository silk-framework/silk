package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User

class Learning {
  def toolbar(xhtml: NodeSeq): NodeSeq = {
    if (User().linkingTask.cache.isRunning) {
      bind("entry", chooseTemplate("choose", "loading", xhtml))
    } else {
      bind("entry", chooseTemplate("choose", "train", xhtml))
    }
  }

  def content(xhtml: NodeSeq): NodeSeq = {
    if (User().linkingTask.cache.isRunning) {
      chooseTemplate("choose", "loading", xhtml)
    } else {
      chooseTemplate("choose", "train", xhtml)
    }
  }
}
