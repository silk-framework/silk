package de.fuberlin.wiwiss.silk.workbench.lift.comet

import net.liftweb.http.CometActor
import net.liftweb.http.js.JsCmds.SetHtml
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.util.task.ValueTask.ValueUpdated
import de.fuberlin.wiwiss.silk.learning.Population
import de.fuberlin.wiwiss.silk.learning.individual.Individual
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.linkspec.{Operator, LinkCondition}
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkspec.input.{PathInput, TransformInput}
import xml.Elem
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Widget which shows the current population.
 */
class LearningWidget extends CometActor {
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
    val individuals = population.individuals.toSeq
    val sortedIndividuals = individuals.sortBy(-_.fitness.score)

    <div>
      <div class="link">
        <div class="link-header heading">
          <div class="link-source">Source: <span class="source-value">aaa</span></div>
          <div class="link-target">Target: <span class="target-value">bbb</span></div>
          <div class="link-confidence">ccc</div>
        </div>
      </div> {
        for((individual, count) <- sortedIndividuals.zipWithIndex) yield {
          renderIndividual(individual, count)
        }
      }
    </div>
  }

//TODO
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
      <div class="link-status"><span>Status</span></div>
      <div class="link-buttons"><span>Correct?</span></div>
    </div>
  }


  /**
   * Renders the content of a single indivual.
   */
  private def renderIndividualContent(individual : Individual) = {
    implicit val prefixes = User().project.config.prefixes

    <div class="link-details" id={getId(individual, "details")}>
      { renderLinkCondition(individual.node.build) }
    </div>
  }

  /**
   * Renders a link condition as a tree.
   */
  private def renderLinkCondition(linkCondition: LinkCondition)(implicit prefixes: Prefixes) = {
    <ul class="details-tree">
    { for(aggregation <- linkCondition.rootOperator.toList) yield renderOperator(aggregation) }
    </ul>
  }

  /**
   * Renders a link condition operator.
   */
  private def renderOperator(op: Operator): Elem = op match  {
    case Aggregation(id, required, weight, operators, aggregator) => {
      <li>
        <span class="aggregation">Aggregation: {aggregator.strategyId} ({id})</span>
          <ul>
          { operators.map(renderOperator) }
          </ul>
      </li>
    }
    case Comparison(id, required, threshold, weight, SourceTargetPair(input1, input2), metric) => {
      <li>
        <span class="comparison">Comparison: {metric.strategyId} ({id})</span>
          <ul>
            { renderOperator(input1) }
            { renderOperator(input2) }
          </ul>
      </li>
    }
    case TransformInput(id, inputs, transformer) => {
      <li>
        <span class="comparison">Transformation: {transformer.strategyId} ({id})</span>
          <ul>
            { inputs.map(renderOperator) }
          </ul>
      </li>
    }
    case PathInput(id, path) => {
      <li>
        <span class="comparison">Input ({id})</span>
          <ul>
            { path.serialize }
          </ul>
      </li>
    }
  }

  /**
   * Generates a new id based on an individual.
   */
  private def getId(individual : Individual, prefix : String = "") = {
    prefix + individual.hashCode
  }
}
