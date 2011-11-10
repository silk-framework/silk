package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.UserDataListener
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentPopulation
import de.fuberlin.wiwiss.silk.learning.individual.Population
import net.liftweb.http.CometActor
import net.liftweb.http.js.JsCmds.SetHtml
import java.util.UUID
import xml.Text

class LearnStatus extends CometActor {

  private val id = UUID.randomUUID.toString

  /**
   * Redraw the widget whenever the current population is updated.
   */
  private val populationListener = new UserDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      partialUpdate(SetHtml(id, renderPopulation(population)))
    }
  }

  /**
   * Renders this widget.
   */
  override def render = {
    <div id={id}>{renderPopulation(CurrentPopulation())}</div>
  }

  private def renderPopulation(population: Population) = {
    if(population.isEmpty)
      Text("Learning not started")
    else
      Text("Fitness: " + population.bestIndividual.fitness.fMeasure)
  }
}