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

import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.util.task.{TaskRunning, TaskFinished, TaskStarted, TaskStatus}
import net.liftweb.http.{CometActor, SHtml}
import java.util.UUID
import net.liftweb.http.js.JsCmds.SetHtml
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningConfiguration, LearningTask}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentConfiguration, CurrentPopulation, CurrentLearningTask}

class RelearnButton extends CometActor {

  /** The id of the HTML button. */
  private val id = UUID.randomUUID.toString

  /** The image which is shown when the button is enabled */
  private val enabledImg = <img src="./static/img/refresh.png" title="Relearn from the current reference links" />

  /** The image which is shown when the button is disabled */
  private val disabledImg = <img src="./static/img/uncorrect.png" title="Cancel learning" />

  def render = {
    SHtml.a(() => learn(), <div id={id}>{if(!CurrentLearningTask().status.isRunning) enabledImg else disabledImg}</div>)
  }

  private def learn() = {
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    } else if(!User().linkingTask.referenceLinks.isDefined) {
      JS.Message("Positive and negative reference links are needed in order to learn a linkage rule")
    } else if (!CurrentLearningTask().status.isRunning) {
      startLearningTask()
      SetHtml(id, disabledImg)
    }
    else {
      CurrentLearningTask().cancel()
      JS.Empty
    }
  }

  private def startLearningTask() {
    val input =
      LearningInput(
        trainingEntities = User().linkingTask.cache.entities,
        seedLinkageRules = List(User().linkingTask.linkSpec.rule)
      )
    val task = new LearningTask(input, CurrentConfiguration())
    CurrentLearningTask() = task
    task.runInBackground()
  }

  /**
   * Listens to changes of the current learning task.
   */
  private val learningTaskListener = new CurrentTaskStatusListener(CurrentLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted =>
        case _: TaskFinished => partialUpdate {
          CurrentPopulation() = task.value.get.population
          SetHtml(id, enabledImg)
        }
        case _: TaskRunning => partialUpdate {
          CurrentPopulation() = task.value.get.population
        }
        case _ =>
      }
    }
  }
}