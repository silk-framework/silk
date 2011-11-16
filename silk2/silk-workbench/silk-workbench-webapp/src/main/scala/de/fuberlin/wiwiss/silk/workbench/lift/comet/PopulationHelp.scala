package de.fuberlin.wiwiss.silk.workbench.lift.comet

/**
 * Help for the population tab.
 */
class PopulationHelp extends Help {

  override def overview = {
    <div>
    The current population of linkage rules which have been learned.
    </div>
  }

  override def actions = {
    <div>
    Choose a linkage rule to be loaded into the editor by pressing <img src="./static/img/learn/load.png"></img>.
    </div>
  }
}