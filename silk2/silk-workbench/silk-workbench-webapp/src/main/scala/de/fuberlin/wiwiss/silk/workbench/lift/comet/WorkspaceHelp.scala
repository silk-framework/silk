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

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.Text

class WorkspaceHelp extends Help {

  private val listener = User().onUpdate(_ => reRender())

  override def overview = {
    <div>
      Use the workspace to manage different projects.
      Each project consists of data sources, linking tasks and output tasks.
    </div>
  }

  override def actions = Text(
    if (User().workspace.projects.isEmpty) {
      "Create a new empty project or import an existing project."
    } else if (!User().projectOpen) {
      "Select the project you want to work on."
    } else if (User().project.sourceModule.tasks.isEmpty) {
      "Add the data sources you want to interlink."
    } else if (User().project.linkingModule.tasks.isEmpty) {
      "Add a linking task."
    } else {
      "Open a linking task for editing."
    }
  )
}