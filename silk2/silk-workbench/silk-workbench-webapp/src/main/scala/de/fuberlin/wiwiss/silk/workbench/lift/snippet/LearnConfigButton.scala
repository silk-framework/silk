package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml

class LearnConfigButton {
  def render(xhtml: NodeSeq): NodeSeq = {
    SHtml.ajaxButton("Advanced", () => LearnConfigDialog.openCmd)
  }
}