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

import xml.NodeSeq
import net.liftweb.http.CometActor

/**
 * A widget which shows context-sensitive help to the current tab.
 * Each inheriting class should provide two functions:
 * renderOverview: A short overview of the current tab
 * renderActions: A list of recommended actions
 */
trait Help extends CometActor {
  /**
   * A short overview of the current tab.
   */
  protected def overview: NodeSeq

  /**
   * A list of recommended actions.
   */
  protected def actions: NodeSeq = NodeSeq.Empty

  /**
   * Renders this widget.
   */
  override def render = {
   <div>
     { renderStatic }
     { renderDynamic }
   </div>
  }

  /**
   * Renders the static part of the help
   */
  private def renderStatic = {
    <b>Overview</b><br/> ++ overview
  }

  /**
   * Renders the dynamic part of the help.
   */
  private def renderDynamic = {
    val nodes = actions
    if(nodes.isEmpty)
      nodes
    else
      <br/><b>Next Steps</b><br/> ++ nodes
  }
}