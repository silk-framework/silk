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

import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentActiveLearningTask, CurrentPopulation}
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, TaskDataListener, User}
import de.fuberlin.wiwiss.silk.util.task.{TaskStarted, TaskStatus}

class LearnHelp extends LinksHelp {

  /**
   * Re-renders the widget if the current linking task has been changed.
   */
  private val linkingTaskListener = User().onUpdate {
    case _: User.CurrentTaskChanged => reRender()
    case _ =>
  }

  /**
   * Re-renders the widget whenever the population has been updated.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      reRender()
    }
  }

  /**
   * Listens to changes of the current active learning task.
   */
  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => reRender()
        case _ =>
      }
    }
  }

  override def overview = {
    <div>
      Learns linkage rules.
    </div>
  }

  override def actions = {
    if(!CurrentActiveLearningTask().status.isRunning && CurrentPopulation().isEmpty) {
      <div>Start the learning by pressing the <em>Start</em> button.</div>
    }
    else if(User().linkingTask.referenceLinks.positive.size + User().linkingTask.referenceLinks.negative.size < 5) {
      <div>
        Rate the links for which the learning algorithm is uncertain:
        { howToRateLinks }
      </div>
    }
    else {
      <div>
        Press the <em>Done</em> button if you are happy with the current linkage rule.
        You can also rate additional links in order to further improve the result:
        { howToRateLinks }
      </div>
    }
  }
}