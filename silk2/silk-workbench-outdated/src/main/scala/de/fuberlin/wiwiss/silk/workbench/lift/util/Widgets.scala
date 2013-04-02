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

import net.liftweb.http.SHtml

import net.liftweb.http.js.JsCmds.{SetHtml, Script, OnLoad}
import JS.PeriodicUpdate
import net.liftweb.http.js.JE.JsRaw
import xml.{NodeBuffer, Text}
import de.fuberlin.wiwiss.silk.util.task.{HasStatus, Task}

object Widgets {
  @deprecated("Use TaskControl instead")
  def taskControl[T](task : Task[T], cancelable : Boolean = false) = {
    def startTask() {
      if(!task.status.isRunning) {
        task.runInBackground()
      }
    }

    var buttons = new NodeBuffer()

    buttons += SHtml.submit("Start", startTask)

    if(cancelable) {
      buttons += SHtml.submit("Cancel", () => task.cancel())
    }

    <form method="POST">
    { buttons }
    </form>
  }

  @deprecated("Use ProgressWidget instead")
  def taskProgress(task : HasStatus) = {
    currentTaskProgress(() => Some(task))
  }

  @deprecated("Use ProgressWidget instead")
  def currentTaskProgress(task : () => Option[HasStatus]) = {
    //Updates the status message
    def update() = task() match {
      case Some(currentTask) => {
        val html = {
          <div id="progressbar"></div>
          <span class="progresstext">{currentTask.status.toString}</span>
        }

        val javascript = "$('#progressbar').progressbar({value: " + (currentTask.status.progress * 95 + 5) + "});"

        SetHtml("status", html) & JsRaw(javascript).cmd
      } case None => {
        SetHtml("status", Text("No task running"))
      }
    }

    <span id="status">Waiting...</span>
    <span>{Script(OnLoad(PeriodicUpdate(update, 1000)))}</span>
  }
}