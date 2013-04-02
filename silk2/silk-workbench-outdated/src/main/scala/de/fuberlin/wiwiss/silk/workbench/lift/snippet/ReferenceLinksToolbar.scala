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

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{ReferenceType, Positive, Negative}
import xml._

class ReferenceLinksToolbar {

  def render(xhtml : NodeSeq) : NodeSeq = {
    def setChecked(input : Elem, linkType : ReferenceType) = {
      if(ShowLinks() == linkType)
        input % Attribute("checked", Text("checked"), Null)
      else
        input
    }

    bind("entry", xhtml,
         "selectLinks" ->
           <div id="selectLinks"> {
             setChecked(<input onchange={SHtml.ajaxInvoke(showLinks(Positive))._2.cmd.toJsCmd} id="showPositiveLinks" type="radio" name="selectLinks" />, Positive) ++
             <label for="showPositiveLinks">Positive</label> ++
             setChecked(<input onchange={SHtml.ajaxInvoke(showLinks(Negative))._2.cmd.toJsCmd} id="showNegativeLinks" type="radio" name="selectLinks" />, Negative) ++
             <label for="showNegativeLinks">Negative</label>
           }
           </div>,
         "import" -> SHtml.ajaxButton("Import", ImportReferenceLinksDialog.openCmd _),
         "export" -> SHtml.ajaxButton("Export", ExportReferenceLinksDialog.openCmd _),
         "download" -> SHtml.ajaxButton("Download", () => JS.Redirect("referenceLinks.xml")),
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Evaluation" target="_help">Help</a>,
         "scripts" -> Script(OnLoad(ImportReferenceLinksDialog.initCmd))
    )
 }

  private def showLinks(linkType : EvalLink.ReferenceType)() = {
    ShowLinks() = linkType
    JS.Empty
  }
}
