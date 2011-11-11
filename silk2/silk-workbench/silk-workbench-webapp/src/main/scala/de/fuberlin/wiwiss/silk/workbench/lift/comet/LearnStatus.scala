package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentPopulation
import de.fuberlin.wiwiss.silk.learning.individual.Population
import net.liftweb.http.CometActor
import net.liftweb.http.js.JsCmds.SetHtml
import java.util.UUID
import xml.Text
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, TaskDataListener}
import de.fuberlin.wiwiss.silk.workbench.lift.util.LinkageRuleTree

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
      Text("No linkage rule learned yet.")
    else
      <div>
        Fitness: {"%.1f".format((population.bestIndividual.fitness.fMeasure) * 100)} (based on {referenceLinks.positive.size} positive and {referenceLinks.negative.size} negative reference links)
        <br/>
        { LinkageRuleTree.render(population.bestIndividual.node.build) }
      </div>
  }
}