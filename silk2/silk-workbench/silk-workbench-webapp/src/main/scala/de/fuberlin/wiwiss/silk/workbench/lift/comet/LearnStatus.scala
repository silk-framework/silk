package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentPopulation
import de.fuberlin.wiwiss.silk.learning.individual.Population
import net.liftweb.http.CometActor
import net.liftweb.http.js.JsCmds.SetHtml
import java.util.UUID
import xml.Text
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, TaskDataListener}

class LearnStatus extends CometActor {

  private val id = UUID.randomUUID.toString

  /**
   * Redraw the widget whenever the current population is updated.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
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
    val referenceLinks = User().linkingTask.referenceLinks
    if(population.isEmpty)
      Text("Learning not started")
    else
      Text("Fitness: " + "%.1f".format((population.bestIndividual.fitness.fMeasure) * 100) +
           "% Pos. Ref. Links: " + referenceLinks.positive.size +
           " Neg. Ref. Links: " + referenceLinks.negative.size)
  }
}