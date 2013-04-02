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

/**
 * Help for the population tab.
 */
class PopulationHelp extends Help {

  override def overview = {
    <div>
    The current population of linkage rules which have been learned.
    </div>
  }

  override def actions = {
    <div>
    Choose a linkage rule to be loaded into the editor by pressing <img src="./static/img/learn/load.png"></img>.
    </div>
  }
}