package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}

/**
 * Help for the learn tab.
 */
class LearnHelp extends Help {

  /**
   * Re-renders the widget if the current linking task has been changed.
   */
  private val linkingTaskListener = User().onUpdate {
    case User.CurrentTaskChanged(_) => reRender()
    case _ =>
  }

  /**
   * Re-renders the widget if a learning task has been started or stopped.
   */
  private val learningTaskListener = new CurrentTaskStatusListener(CurrentLearningTask) {
    override def onUpdate(status: TaskStatus) = status match {
      case _: TaskStarted | _: TaskFinished => reRender()
      case _ =>
    }
  }

  override def overview = {
    <div>
      Learns linkage rules from reference links.
    </div>
  }

  override def actions = {
    if(!User().linkingTask.referenceLinks.isComplete) {
      <div>
        Reference links are needed in order to be able to learn linkage rules.
        Go to the <em>Reference Links</em> Tab.
      </div>
    } else if(CurrentLearningTask().status.isRunning) {
      <div>
        Wait until you are happy with the results and press the <em>Stop</em> button.
        The learning will stop automatically as soon as either the full f-Measure is reached or the maximum number of iterations has been exceeded.
      </div>
    } else if(CurrentLearningTask().result.population.isEmpty) {
      <div>
        Start the learning by pressing the <em>Start</em> button.
      </div>
    } else {
      <div>
        Select a linkage rule and load it into the editor.
        If no good linkage rules have been found, check if the reference links are good and start a new learning run.
      </div>
    }
  }
}