package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPopulation, CurrentLearningTask}
import de.fuberlin.wiwiss.silk.util.task.{TaskRunning, TaskFinished, TaskStarted, TaskStatus}
import net.liftweb.http.{CometActor, SHtml}
import java.util.UUID
import net.liftweb.http.js.JsCmds.SetHtml
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningConfiguration, LearningTask}

class RelearnButton extends CometActor {

  /** The id of the HTML button. */
  private val id = UUID.randomUUID.toString

  /** The image which is shown when the button is enabled */
  private val enabledImg = <img src="./static/img/refresh.png" title="Relearn from the current reference links" />

  /** The image which is shown when the button is disabled */
  private val disabledImg = <img src="./static/img/uncorrect.png" title="Cancel learning" />

  def render = {
    SHtml.a(() => learn(), <div id={id}>{enabledImg}</div>)
  }

  private def learn() = {
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    } else if(!User().linkingTask.referenceLinks.isDefined) {
      JS.Message("Positive and negative reference links are needed in order to learn a linkage rule")
    } else if (!CurrentLearningTask().status.isRunning) {
      startLearningTask()
      SetHtml(id, disabledImg)
    }
    else {
      CurrentLearningTask().cancel()
      JS.Empty
    }
  }

  private def startLearningTask() {
    val input =
      LearningInput(
        trainingEntities = User().linkingTask.cache.entities,
        seedLinkageRules = List(User().linkingTask.linkSpec.rule)
      )
    val config = LearningConfiguration.load()
    val task = new LearningTask(input, config)
    CurrentLearningTask() = task
    task.runInBackground()
  }

  /**
   * Listens to changes of the current learning task.
   */
  private val learningTaskListener = new CurrentTaskStatusListener(CurrentLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted =>
        case _: TaskFinished => partialUpdate {
          CurrentPopulation() = task.value.get.population
          SetHtml(id, enabledImg)
        }
        case _: TaskRunning => partialUpdate {
          CurrentPopulation() = task.value.get.population
        }
        case _ =>
      }
    }
  }
}