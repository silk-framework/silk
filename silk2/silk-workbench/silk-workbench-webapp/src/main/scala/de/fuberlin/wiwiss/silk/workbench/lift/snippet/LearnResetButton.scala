package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.learning._
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS

class LearnResetButton {
  def render(xhtml: NodeSeq): NodeSeq = {
    SHtml.ajaxButton("Reset", () => reset())
  }

  private def reset() = {
    CurrentPool.reset()
    CurrentPopulation.reset()
    CurrentValidationLinks.reset()
    CurrentActiveLearningTask().cancel()

    JS.Empty
  }
}