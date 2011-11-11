package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.{TaskDataListener, User}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPopulation, CurrentLearningTask}
import de.fuberlin.wiwiss.silk.learning.individual.Population

/**
 * Help for the population tab.
 */
class PopulationHelp extends Help {

//  /**
//   * Re-renders the widget whenever the population has been updated.
//   */
//  private val populationListener = new TaskDataListener(CurrentPopulation) {
//    override def onUpdate(population: Population) {
//      reRender()
//    }
//  }

  override def overview = {
    <div>
      The current population of linkage rules which have been learned.
    </div>
  }
}