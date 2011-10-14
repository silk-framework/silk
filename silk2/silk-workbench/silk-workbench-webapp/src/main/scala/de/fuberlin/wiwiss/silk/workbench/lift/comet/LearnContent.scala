package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.{NodeSeq, Elem}
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.{Call, JsRaw}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.learning._
import de.fuberlin.wiwiss.silk.learning.individual.{Population, Individual}
import de.fuberlin.wiwiss.silk.learning.{LearningTask, LearningResult}
import de.fuberlin.wiwiss.silk.evaluation.statistics.LinkageRuleComplexity
import de.fuberlin.wiwiss.silk.learning.LearningResult.Finished
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStatus}
import net.liftweb.http.js.JsCmds.{Confirm, OnLoad, SetHtml, Script}
import de.fuberlin.wiwiss.silk.workbench.workspace.{UserDataListener, CurrentTaskStatusListener, CurrentTaskValueListener, User}

/**
 * Widget which shows the current population.
 */
class LearnContent extends CometActor {

  /** The individuals to be rendered. */
  private def individuals = CurrentPopulation().individuals

  /** The number of links shown on one page. */
  private val pageSize = 100

  /** Redraw the widget on every view, because the current learning task may change. */
  override protected val dontCacheRendering = true

  /**
   * Redraw the widget whenever the current population is updated.
   */
  private val populationListener = new UserDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      partialUpdate(updateListCmd)
    }
  }

  /**
   * Renders this widget.
   */
  override def render = {
    val showListFunc = JsCmds.Function("showList", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showList(pageStr.toInt))._2.cmd)

    bind("entry", defaultHtml,
         "script" -> Script(OnLoad(updateListCmd) & showListFunc),
         "list" -> <div id="results" />)
  }

  private def updateListCmd: JsCmd = {
    JsRaw("initPagination(" + individuals.size + ");").cmd
  }

  private def showList(page: Int): JsCmd = {
    val sortedIndividuals = PopulationSorter.sort(individuals.toSeq)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    SetHtml("results", renderPopulation(pageIndividuals)) & Call("initTrees").cmd & Call("updateResultsWidth").cmd
  }

  /**
   * Renders the population.
   */
  private def renderPopulation(individuals: Seq[Individual]) = {
    <div>
      <div class="individual">
        <div class="individual-header heading">
          <div class="individual-desc">Description</div>
          <div class="individual-score">{renderSortHeader("Score", ScoreSorterAscending, ScoreSorterDescending)}</div>
          <div class="individual-mcc">{renderSortHeader("MCC", MccSorterAscending, MccSorterDescending)}</div>
          <div class="individual-f1">{renderSortHeader("F-Measure", FMeasureSorterAscending, FMeasureSorterDescending)}</div>
          <div class="individual-buttons">Actions</div>
        </div>
      </div> {
        for((individual, count) <- individuals.zipWithIndex) yield {
          renderIndividual(individual, count)
        }
      }
    </div>
  }

  private def renderSortHeader(label: String, ascendingSorter: PopulationSorter, descendingSorter: PopulationSorter) = {
    def sort() = {
      if (PopulationSorter() == descendingSorter) {
        PopulationSorter() = ascendingSorter
      } else {
        PopulationSorter() = descendingSorter
      }
      updateListCmd
    }

    val icon = PopulationSorter() match {
      case `ascendingSorter` => "./static/img/sort-ascending.png"
      case `descendingSorter` => "./static/img/sort-descending.png"
      case _ => "./static/img/sort.png"
    }

    SHtml.a(sort _, <span>{label}<img src={icon}/></span>)
  }

  /**
   * Renders a single individual.
   */
  private def renderIndividual(individual: Individual, counter: Int) = {
    <div class="individual" id={getId(individual)} >
      { renderIndividualHeader(individual, counter) }
      { renderIndividualContent(individual) }
      <div style="clear:both"></div>
    </div>
  }

  /**
   * Renders the list header of a single individual.
   */
  private def renderIndividualHeader(individual: Individual, counter: Int) = {
    <div class={if (counter%2==0) "individual-header grey" else "individual-header" }
         onmouseover="$(this).addClass('individual-over');"
         onmouseout="$(this).removeClass('individual-over');">
      <div id={getId(individual, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
      <div class="individual-desc">{renderDescription(individual)}</div>
      <div class="individual-score">{renderScore(individual.fitness.score)}</div>
      <div class="individual-mcc">{renderScore(individual.fitness.mcc)}</div>
      <div class="individual-f1">{renderScore(individual.fitness.fMeasure)}</div>
      <div class="individual-buttons">{renderButtons(individual)}</div>
    </div>
  }

  /**
   * Renders the description of an individual.
   */
  private def renderDescription(individual: Individual) = {
    val complexity = LinkageRuleComplexity(individual.node.build)

    complexity.comparisonCount + " Comparisons and " + complexity.transformationCount + " Transformations"
  }

  /**
   * Renders a score between -1.0 and 1.0.
   */
  private def renderScore(score: Double): NodeSeq = {
    <div class="confidencebar">
      <div class="confidence">{"%.1f".format(score * 100)}%</div>
    </div>
  }

  /**
   * Renders the action buttons for an individual.
   */
  private def renderButtons(individual: Individual) = {
    val image = <img src="./static/img/learn/load.png" title="Load this linkage rule in the editor" />

    SHtml.a(() => loadIndividualCmd(individual), image)
  }

  /**
   * Renders the content of a single indivual.
   */
  private def renderIndividualContent(individual: Individual) = {
    implicit val prefixes = User().project.config.prefixes

    <div class="individual-details" id={getId(individual, "details")}>
      { renderLinkageRule(individual.node.build) }
    </div>
  }

  /**
   * Renders a link condition as a tree.
   */
  private def renderLinkageRule(rule: LinkageRule)(implicit prefixes: Prefixes) = {
    <ul class="details-tree">
    { for(aggregation <- rule.operator.toList) yield renderOperator(aggregation) }
    </ul>
  }

  /**
   * Renders a link condition operator.
   */
  private def renderOperator(op: Operator): Elem = op match  {
    case Aggregation(id, required, weight, aggregator, operators) => {
      <li>
        <span class="aggregation">Aggregation: {aggregator.pluginId}</span>
        <ul>
        { operators.map(renderOperator) }
        </ul>
      </li>
    }
    case Comparison(id, required, weight, threshold, metric, DPair(input1, input2)) => {
      <li>
        <span class="comparison">Comparison: {metric.pluginId} ({threshold.toString})</span>
        <ul>
          { renderOperator(input1) }
          { renderOperator(input2) }
        </ul>
      </li>
    }
    case TransformInput(id, transformer, inputs) => {
      <li>
        <span class="transformation">Transformation: {transformer.pluginId}</span>
        <ul>
          { inputs.map(renderOperator) }
        </ul>
      </li>
    }
    case PathInput(id, path) => {
      <li>
        <span class="input">Input: {path.serialize}</span>
      </li>
    }
  }

  def loadIndividualCmd(individual: Individual) = {
    def load() = {
      val linkingTask = User().linkingTask
      val linkSpec = linkingTask.linkSpec
      val newLinkageRule = individual.node.build

      User().task = linkingTask.updateLinkSpec(linkSpec.copy(rule = newLinkageRule), User().project)

      JS.Redirect("/editor.html")
    }

    Confirm("This will overwrite the current linkage rule!", SHtml.ajaxInvoke(load)._2.cmd)
  }

  /**
   * Generates a new id based on an individual.
   */
  private def getId(individual : Individual, prefix : String = "") = {
    prefix + individual.hashCode
  }
}
