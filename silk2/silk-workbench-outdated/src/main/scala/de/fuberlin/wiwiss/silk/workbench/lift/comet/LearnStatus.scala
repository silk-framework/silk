/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentPopulation
import de.fuberlin.wiwiss.silk.learning.individual.Population
import net.liftweb.http.CometActor
import net.liftweb.http.js.JsCmds.SetHtml
import java.util.UUID
import xml.Text
import de.fuberlin.wiwiss.silk.workspace.{User, TaskDataListener}
import de.fuberlin.wiwiss.silk.workbench.lift.util.LinkageRuleTree
import net.liftweb.http.js.JE.JsRaw

class LearnStatus extends CometActor {

  private val id = UUID.randomUUID.toString

  override protected val dontCacheRendering = true

  /**
   * Redraw the widget whenever the current population is updated.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      partialUpdate(SetHtml(id, renderPopulation(population)) & JsRaw("initTrees()").cmd)
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
        Fitness: {"%.1f".format((population.bestIndividual.fitness) * 100)} (based on {referenceLinks.positive.size} positive and {referenceLinks.negative.size} negative reference links)
        <br/>
        { LinkageRuleTree.render(population.bestIndividual.node.build) }
      </div>
  }
}