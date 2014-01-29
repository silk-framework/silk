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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.BindHelpers._
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningTask}
import de.fuberlin.wiwiss.silk.workbench.learning._
import de.fuberlin.wiwiss.silk.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.runtime.task.{TaskRunning, TaskStarted, TaskFinished, TaskStatus}

class LearnContent {
  def render(xhtml: NodeSeq): NodeSeq = {
    if(CurrentActiveLearningTask().isEmpty && CurrentLearningTask().isEmpty)
      chooseTemplate("choose", "start", xhtml) ++ renderStart()
    else
      chooseTemplate("choose", "learn", xhtml)
  }

  private def renderStart() = {
    val posCount = User().linkingTask.referenceLinks.positive.size
    val negCount = User().linkingTask.referenceLinks.negative.size

    if(User().linkingTask.referenceLinks.isDefined)
      <span>
        There are already {posCount} positive and {negCount} negative reference links.
        Start learning a linkage rules from existing reference links: {SHtml.ajaxButton("Start", startLearning(false))}
        <br/>
        Start with existing reference links but find new reference links: {SHtml.ajaxButton("Start", startLearning(true))}
      </span>
    else
      <span>
        Start active learning: {SHtml.ajaxButton("Start", startLearning(true))}
      </span>
  }

  private def startLearning(activeLearning: Boolean) = () => {
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    } else if(activeLearning) {
      //Start active learning task
      val sampleLinksTask =
        new ActiveLearningTask(
          config = CurrentConfiguration(),
          sources = User().project.sourceModule.tasks.map(_.source),
          linkSpec = User().linkingTask.linkSpec,
          paths = User().linkingTask.cache.entityDescs.map(_.paths),
          referenceEntities = User().linkingTask.cache.entities,
          pool = CurrentPool(),
          population = CurrentPopulation()
        )

      CurrentActiveLearningTask() = sampleLinksTask
      sampleLinksTask.runInBackground()
      JS.Reload
    } else {
      //Start passive learning task
      val input =
        LearningInput(
          trainingEntities = User().linkingTask.cache.entities,
          seedLinkageRules = List(User().linkingTask.linkSpec.rule)
        )
      val task = new LearningTask(input, CurrentConfiguration())
      CurrentLearningTask() = task
      task.runInBackground()
      JS.Reload
    }
  }
}


