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

import de.fuberlin.wiwiss.silk.workspace.User
import xml.{NodeSeq, Text}

class ReferenceLinksHelp extends LinksHelp {

  /**
   * Re-renders the widget if the current linking task (and with it the reference links) has been changed.
   */
  private val taskListener = User().onUpdate {
    case _: User.CurrentTaskChanged => reRender()
    case _ =>
  }

  override def overview = {
    <div>
      The reference links of this linking task.
      Positive reference links represent definitive matches, while negative reference links represent definitive non-matches.
      The reference links are used for evaluating the quality of the current linkage rule as well as for learning new linkage rules.
    </div>
  }

  override def actions = {
    val links = User().linkingTask.referenceLinks
    if(links.isEmpty) {
      Text("This linking task does not contain any reference links yet.") ++
      howToAddReferenceLinks
    } else if(links.positive.isEmpty) {
      Text("This linking task does not contain any positive reference links yet.") ++
      howToAddReferenceLinks
    } else if(links.negative.isEmpty) {
      Text("This linking task does not contain any negative reference links yet.") ++
      howToAddReferenceLinks
    } else {
      NodeSeq.Empty
    }
  }
}