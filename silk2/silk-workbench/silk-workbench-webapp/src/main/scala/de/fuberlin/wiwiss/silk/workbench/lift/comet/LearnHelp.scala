package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.{TaskDataListener, User}
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentPopulation
import de.fuberlin.wiwiss.silk.learning.individual.Population

class LearnHelp extends LinksHelp {

  /**
   * Re-renders the widget if the current linking task has been changed.
   */
  private val linkingTaskListener = User().onUpdate {
    case _: User.CurrentTaskChanged => reRender()
    case _ =>
  }

  /**
   * Re-renders the widget whenever the population has been updated.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      reRender()
    }
  }

  override def overview = {
    <div>
      Learns linkage rules.
    </div>
  }

  override def actions = {
    if(CurrentPopulation().isEmpty) {
      <div>Start the learning by pressing the <em>Start</em> button.</div>
    }
    else if((User().linkingTask.referenceLinks.positive.size < 5 || User().linkingTask.referenceLinks.negative.size == 0) &&
            (User().linkingTask.referenceLinks.negative.size < 5 || User().linkingTask.referenceLinks.positive.size == 0)) {
      <div>
        Rate the links for which the learning algorithm is uncertain:
        { howToRateLinks }
      </div>
    }
    else {
      <div>
        Press the <em>Done</em> button if you are happy with the current linkage rule.
        You can also rate additional links in order to further improve the result:
        { howToRateLinks }
      </div>
    }
  }
}