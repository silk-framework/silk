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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.learning._
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS

class LearnResetButton {
  def render(xhtml: NodeSeq): NodeSeq = {
    SHtml.ajaxButton("Reset", () => reset())
  }

  private def reset() = {
    CurrentPool.reset()
    CurrentPopulation.reset()
    CurrentValidationLinks.reset()
    CurrentActiveLearningTask().cancel()
    CurrentLearningTask().cancel()
    CurrentActiveLearningTask.reset()
    CurrentLearningTask.reset()

    JS.Reload
  }
}