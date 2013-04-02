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

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentValidationLinks
import de.fuberlin.wiwiss.silk.workspace.{TaskDataListener, User}
import de.fuberlin.wiwiss.silk.entity.Link

class LearnHelp extends LinksHelp {

  /**
   * Listens to changes of the current validation links.
   */
  private val validationListener = new TaskDataListener(CurrentValidationLinks) {
    override def onUpdate(status: Seq[Link]) {
      reRender()
    }
  }

  override def overview = {
    <div>
      Learns linkage rules.
    </div>
  }

  override def actions = {
    if(CurrentValidationLinks().isEmpty) {
      <div>Wait until you are happy with the result and press the <em>Done</em> button</div>
    }
    else {
      if(User().linkingTask.referenceLinks.positive.size + User().linkingTask.referenceLinks.negative.size < 5) {
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
}