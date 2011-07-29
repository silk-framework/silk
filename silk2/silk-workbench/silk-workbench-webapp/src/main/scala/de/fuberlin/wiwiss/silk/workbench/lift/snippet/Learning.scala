package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.learning.CrossValidationTask

class Learning {
  def toolbar(xhtml: NodeSeq): NodeSeq = {
    if (User().linkingTask.cache.isRunning) {
      bind("entry", chooseTemplate("choose", "loading", xhtml),
           "startCrossValidation" -> NodeSeq.Empty)
    } else {
      bind("entry", chooseTemplate("choose", "train", xhtml),
           "startCrossValidation" -> SHtml.button("Cross validation", () => new CrossValidationTask(User().linkingTask.cache.instances).run()))
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
