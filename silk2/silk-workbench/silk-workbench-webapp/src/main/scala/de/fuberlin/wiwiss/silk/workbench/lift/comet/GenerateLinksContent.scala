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

import de.fuberlin.wiwiss.silk.util.task._
import net.liftweb.http.SHtml
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Correct, Incorrect, Unknown, Generated}
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, EvalLink}
import de.fuberlin.wiwiss.silk.GenerateLinksTask
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedEvaluator

class GenerateLinksContent extends LinksContent with RateLinkButtons {

  /**Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /**The time of the last update */
  private var lastUpdateTime = 0L

  override protected val showStatus = false

  private var generateLinksTask = CurrentGenerateLinksTask()

  private val currentGenerateLinksTaskListener = (task: GenerateLinksTask) => { generateLinksTask = task }

  CurrentGenerateLinksTask.onUpdate(currentGenerateLinksTaskListener)

  private val generatedLinkListener = new CurrentTaskStatusListener(CurrentGenerateLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => {}
        case _: TaskRunning if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod => {
          partialUpdate(updateLinksCmd)
          lastUpdateTime = System.currentTimeMillis
        }
        case _: TaskFinished => {
          val cmd = {
            val warnings = CurrentGenerateLinksTask().warnings
            if (warnings.isEmpty) {
              updateLinksCmd
            }
            else {
              updateLinksCmd & Alert("Warnings have been raised during execution:\n- " + warnings.map(_.getMessage).mkString("\n- "))
            }
          }

          partialUpdate(cmd)
        }
        case _ =>
      }
    }
  }

  override protected def links: Seq[EvalLink] = {
    def referenceLinks = linkingTask.referenceLinks

    for (link <- generateLinksTask.links.view) yield {
      val detailedLink = DetailedEvaluator(linkingTask.linkSpec.rule, link.entities.get).get
      if (referenceLinks.positive.contains(link)) {
        new EvalLink(detailedLink, Correct, Generated)
      } else if (referenceLinks.negative.contains(link)) {
        new EvalLink(detailedLink, Incorrect, Generated)
      } else {
        new EvalLink(detailedLink, Unknown, Generated)
      }
    }
  }

  override protected def renderStatus(link: EvalLink): NodeSeq = {
    link.correct match {
      case Correct => <div>correct</div>
      case Incorrect => <div>wrong</div>
      case Unknown => <div>unknown</div>
    }
  }
}
