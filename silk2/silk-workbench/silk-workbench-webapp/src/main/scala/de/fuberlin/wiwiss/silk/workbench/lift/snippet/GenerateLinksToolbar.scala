package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._

class GenerateLinksToolbar {
  def render(xhtml : NodeSeq) : NodeSeq = {
    bind("entry", xhtml,
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Evaluation" target="_help">Help</a>
    )
  }
}
