package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.learning.CrossValidationTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User

class LearnToolbar {

  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml,
         "startCrossValidation" -> SHtml.button("Cross validation", () => new CrossValidationTask(User().linkingTask.cache.instances).run()))
  }
}
