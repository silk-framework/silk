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

import de.fuberlin.wiwiss.silk.workbench.lift.util.DynamicButton
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton, TaskControl}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.GenerateLinksDialog
import de.fuberlin.wiwiss.silk.workspace.{CurrentTaskStatusListener, User, TaskData}
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds.Alert

class GenerateLinksControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if (!CurrentGenerateLinksTask().status.isRunning)
      GenerateLinksDialog.openCmd
    else {
      CurrentGenerateLinksTask().cancel()
      JS.Empty
    }
  }

  /**
   * Listens to changes of the current learning task.
   */
  private val generateLinksTaskListener = new CurrentTaskStatusListener(CurrentGenerateLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => label = "Stop"
        case _: TaskFinished => {
          label = "Start"
          val warnings = task.warnings
          if(!warnings.isEmpty)
            partialUpdate(Alert("There have been warnings while generating the links: " + warnings.map(_.getMessage).mkString("\n")))
        }
        case _ =>
      }
    }
  }
}