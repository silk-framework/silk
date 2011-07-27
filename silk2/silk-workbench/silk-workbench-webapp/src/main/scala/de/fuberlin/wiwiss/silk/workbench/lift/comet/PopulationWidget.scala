package de.fuberlin.wiwiss.silk.workbench.lift.comet

import net.liftweb.http.CometActor
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import net.liftweb.http.js.JsCmds.SetHtml
import collection.mutable.{Publisher, Subscriber}
import xml.{NodeBuffer, NodeSeq}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.util.task.ValueTask.ValueUpdated
import de.fuberlin.wiwiss.silk.learning.Population
import de.fuberlin.wiwiss.silk.learning.individual.Individual
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask

/**
 * Widget which shows the current population.
 */
class PopulationWidget extends CometActor {
  /**
   * Redraw the widget on every view, because the current learning task may change.
   */
  override protected val dontCacheRendering = true

  /**
   * Subscribe to events of the current learning task.
   * Whenever the population is changed the learning tasks fires an event on which we redraw the widget.
   */
  CurrentLearningTask().value.subscribe(new Subscriber[ValueUpdated[Population], Publisher[ValueUpdated[Population]]] {
    def notify(pub : Publisher[ValueUpdated[Population]], event : ValueUpdated[Population]) {
      partialUpdate(SetHtml("results", renderPopulation(event.value)))
    }
  })

  /**
   * Renders this widget.
   */
  override def render = {
    bind("entry", defaultHtml,
         "list" -> <div id="results" />)
  }

  /**
   * Renders the population.
   */
  private def renderPopulation(population: Population) = {
    <div>
      <div class="link">
        <div class="link-header heading">
          <div class="link-source">Source: <span class="source-value">aaa</span></div>
          <div class="link-target">Target: <span class="target-value">bbb</span></div>
          <div class="link-confidence">ccc</div>
        </div>
      </div> {
        for((individual, count) <- population.individuals.toList.zipWithIndex) yield {
          renderIndividual(individual, count)
        }
      }
    </div>
  }

//  def load() =
//  {
//    val linkingTask = User().linkingTask
//    val linkSpec = linkingTask.linkSpec
//    val newLinkCondition = individual.node.build
//
//    User().task = linkingTask.updateLinkSpec(linkSpec.copy(condition = newLinkCondition), User().project)
//
//    JS.Redirect("/linkSpec.html")
//  }

  /**
   * Renders a single individual.
   */
  private def renderIndividual(individual : Individual, counter: Int) = {
    <div class="link" id={getId(individual)} >
      { renderIndividualHeader(individual, counter) }
      { renderIndividualContent(individual) }
      <div style="clear:both"></div>
    </div>
  }

  /**
   * Renders the list header of a single individual.
   */
  private def renderIndividualHeader(individual : Individual, counter: Int) = {
    <div class={if (counter%2==0) "link-header grey" else "link-header" } onmouseover="$(this).addClass('link-over');" onmouseout="$(this).removeClass('link-over');">
      <div id={getId(individual, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
      <div class="link-source">Dummy</div>
      <div class="link-target">Dummy</div>
      <div class="link-confidence">{individual.fitness.mcc.toString}</div>
    </div>
  }

  /**
   * Renders the content of a single indivual.
   */
  private def renderIndividualContent(individual : Individual) = {
    <div class="link-details" id={getId(individual, "details")}>
      { individualToHtml(individual) }
    </div>
  }

  private def individualToHtml(individual : Individual) : NodeSeq = {
    val nodes = new NodeBuffer()

    //Format fitness
    nodes += <div>{"Fitness: " + individual.fitness}</div>

    //Format time
    //nodes += <div>{"time: " + individual.time}</div>

    //Format the condition
    val linkCondition = individual.node.build
    implicit val prefixes = User().project.config.prefixes

    nodes += <pre><tt>{linkCondition.toXML.toFormattedString}</tt></pre>

    //Format the base operator
//    for(Individual.Base(operator, baseIndividual) <- individual.base) {
//      nodes += <div>{"Operator: " + operator}</div>
//      nodes ++= individualToHtml(baseIndividual)
//    }

    //Return nodes
    nodes
  }

  /**
   * Generates a new id based on an individual.
   */
  private def getId(individual : Individual, prefix : String = "") = {
    prefix + individual.hashCode
  }
}
