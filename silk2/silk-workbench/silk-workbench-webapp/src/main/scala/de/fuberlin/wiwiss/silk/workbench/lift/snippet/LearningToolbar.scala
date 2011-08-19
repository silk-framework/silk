package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.util.task.{Finished, Running, Started, Status}
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.learning.{LearningTask, CrossValidationTask}

class LearningToolbar {

  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml,
         "startCrossValidation" -> SHtml.button("Cross validation", () => new CrossValidationTask(User().linkingTask.cache.instances).run()))
  }
}
