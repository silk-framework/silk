package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.BindHelpers._

class LearnToolbar {
  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml)
  }
}