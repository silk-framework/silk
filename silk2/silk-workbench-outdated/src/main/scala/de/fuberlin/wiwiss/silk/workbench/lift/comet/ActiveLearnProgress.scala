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

import de.fuberlin.wiwiss.silk.workspace.CurrentTaskStatusListener
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentValidationLinks, CurrentPopulation, CurrentPool, CurrentActiveLearningTask}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, ProgressWidget}

class ActiveLearnProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentActiveLearningTask), hide = true) {
 /**
  * Listens to changes of the current active learning task.
  */
  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => partialUpdate {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
          JS.Empty
        }
        case _ =>
      }
    }
  }

}