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

package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.runtime.task.{TaskStatus, TaskFinished, TaskStarted, Task}

class TaskControl(task: Task[_], cancelable: Boolean = false) extends DynamicButton {

  task.onUpdate(TaskListener)

  label = "Start"

  override protected def onPressed() = {
    if (!task.status.isRunning) {
      task.runInBackground()
    } else if (cancelable) {
      task.cancel()
    }
    JS.Empty
  }

  private object TaskListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      status match {
        case _: TaskStarted if cancelable => label = "Stop"
        case _: TaskStarted => label = "Start"
        case _: TaskFinished => label = "Start"
        case _ =>
      }
    }
  }
}