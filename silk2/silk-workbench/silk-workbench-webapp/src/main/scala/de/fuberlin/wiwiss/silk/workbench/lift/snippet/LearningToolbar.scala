package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.task.{Finished, Running, Started, Status}
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.learning.{LearningTask, CrossValidationTask}

class LearningToolbar {

  /**Register to status messages of the cache loader task in order to be notified when new links are available */
  User().linkingTask.cache.subscribe(new Subscriber[Status, Publisher[Status]] {
    def notify(pub: Publisher[Status], status: Status) {
      status match {
        case _: Started =>
        case _: Running =>
        case _: Finished => {
          CurrentLearningTask() = new LearningTask(User().linkingTask.cache.instances)
        }
        case _ =>
      }
    }
  })

  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml,
         "startCrossValidation" -> SHtml.button("Cross validation", () => new CrossValidationTask(User().linkingTask.cache.instances).run()))
  }
}
