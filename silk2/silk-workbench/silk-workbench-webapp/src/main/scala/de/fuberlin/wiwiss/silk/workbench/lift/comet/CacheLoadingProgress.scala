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

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{Task, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.{Message, CurrentTaskChanged}

/**
 * Shows the progress of the cache loader task.
 */
class CacheLoadingProgress() extends ProgressWidget(new CacheStatus, hide = true)

/**
 * Listens to cache status changes of the current linking task.
 */
private class CacheStatus extends Observable[TaskStatus] {
  /** Listen to the cache status of the current task. */
  if(User().linkingTaskOpen) User().linkingTask.cache.onUpdate(StatusListener)

  /** Listen to changes of the current task. */
  User().onUpdate(CurrentTaskListener)

  override def onUpdate[U](f: TaskStatus => U) = {
    if(User().linkingTaskOpen) f(User().linkingTask.cache.status)
    super.onUpdate(f)
  }

  private object CurrentTaskListener extends (Message => Unit) {
    def apply(event: Message) {
      event match {
        case CurrentTaskChanged(_, _, task: LinkingTask) => task.cache.onUpdate(StatusListener)
        case _ =>
      }
    }
  }

  private object StatusListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      publish(status)
    }
  }
}