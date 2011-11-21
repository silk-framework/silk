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

trait LinksHelp extends Help {

  protected def howToRateLinks = {
    <div>
      Based on its correctness, each link can be associated to one of the following 3 categories:
      <br/>
      <img src="./static/img/confirm.png"></img>
      Confirms the link as correct. Confirmed links are part of the positive reference link set.
      <br/>
      <img src="./static/img/undecided.png"></img>
      Link whose correctness is unknown.
      <br/>
      <img src="./static/img/decline.png"></img>
      Confirms the link as incorrect. Incorrect links are part of the negative reference link set.
    </div>
  }

  protected def howToAddReferenceLinks = {
    <div>
      You can add reference links the following ways:
      <ul>
        <li>Import existing reference links</li>
        <li>Using the <em>Learn</em> Tab</li>
        <li>Using the <em>Generate Links</em> Tab</li>
      </ul>
    </div>
  }
}