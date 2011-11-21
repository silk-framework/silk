/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import xml.NodeSeq
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.{Call, JsRaw}
import de.fuberlin.wiwiss.silk.workbench.learning._
import de.fuberlin.wiwiss.silk.learning.individual.{Population, Individual}
import de.fuberlin.wiwiss.silk.evaluation.statistics.LinkageRuleComplexity
import net.liftweb.http.js.JsCmds.{Confirm, OnLoad, SetHtml, Script}
import de.fuberlin.wiwiss.silk.workbench.workspace.{TaskDataListener, User}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{LinkageRuleTree, JS}
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.Timer

/**
 * Widget which shows the current population.
 */
class PopulationContent extends CometActor {

  /** The individuals to be rendered. */
  private def individuals = CurrentPopulation().individuals

  /** The number of links shown on one page. */
  private val pageSize = 20

  /** Redraw the widget on every view, because the current learning task may change. */
  override protected val dontCacheRendering = true

  /**
   * Redraw the widget whenever the current population is updated.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
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
    val sortedIndividuals = individuals.toSeq.sortBy(-_.fitness)
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
          <div class="individual-score">Score</div>
          <div class="individual-mcc">MCC</div>
          <div class="individual-f1">F-Measure</div>
          <div class="individual-buttons">Actions</div>
        </div>
      </div> {
        for((individual, count) <- individuals.zipWithIndex) yield {
          renderIndividual(individual, count)
        }
      }
    </div>
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
    val scores = LinkageRuleEvaluator(individual.node.build, User().linkingTask.cache.entities)

    <div class={if (counter%2==0) "individual-header grey" else "individual-header" }
         onmouseover="$(this).addClass('individual-over');"
         onmouseout="$(this).removeClass('individual-over');">
      <div id={getId(individual, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
      <div class="individual-desc">{renderDescription(individual)}</div>
      <div class="individual-score">{renderScore(individual.fitness)}</div>
      <div class="individual-mcc">{renderScore(scores.mcc)}</div>
      <div class="individual-f1">{renderScore(scores.fMeasure)}</div>
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
      { LinkageRuleTree.render(individual.node.build) }
    </div>
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
