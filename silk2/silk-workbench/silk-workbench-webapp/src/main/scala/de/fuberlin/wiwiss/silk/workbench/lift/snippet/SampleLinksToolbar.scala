package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.BindHelpers._
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentSampleLinksTask, CurrentLearningTask, SampleLinksTask}

class SampleLinksToolbar {
  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml)
  }
}