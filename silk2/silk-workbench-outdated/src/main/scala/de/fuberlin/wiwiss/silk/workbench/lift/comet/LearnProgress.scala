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
import de.fuberlin.wiwiss.silk.runtime.task.{TaskRunning, TaskFinished, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPopulation, CurrentLearningTask}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, ProgressWidget}

class LearnProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentLearningTask), hide = true) {
 /**
   * Listens to changes of the current learning task.
   */
  private val learningTaskListener = new CurrentTaskStatusListener(CurrentLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted =>
        case _: TaskFinished => partialUpdate {
          CurrentPopulation() = task.value.get.population
          JS.Empty
        }
        case _: TaskRunning => partialUpdate {
          CurrentPopulation() = task.value.get.population
          JS.Empty
        }
        case _ =>
      }
    }
  }
}
