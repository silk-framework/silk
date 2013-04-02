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
import de.fuberlin.wiwiss.silk.workspace.User
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS

/**
 * A dialog to create new projects.
 */
class CreateProjectDialog {
  def render(xhtml: NodeSeq): NodeSeq = {
    var name = "New_Project"

    def submit() = {
      try {
        User().workspace.createProject(name)

        JsRaw("$('#createProjectDialog').dialog('close');").cmd & Workspace.updateCmd
      } catch {
        case ex: Exception => Workspace.hideLoadingDialogCmd & JS.Message(ex.getMessage)
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
        "name" -> SHtml.text(name, name = _, "id" -> "projectName", "size" -> "60", "title" -> "Project name"),
        "submit" -> SHtml.ajaxSubmit("Create", submit)))
  }
}

object CreateProjectDialog {
  def initCmd = OnLoad(JsRaw("$('#createProjectDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  def openCmd = JsRaw("$('#projectName').val('');").cmd & JsRaw("$('#createProjectDialog').dialog('open');").cmd
}