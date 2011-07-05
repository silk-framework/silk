package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{ReferenceType, Positive, Negative}
import xml._

class ReferenceLinks
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    def setChecked(input : Elem, linkType : ReferenceType) =
    {
      if(ShowLinks() == linkType)
      {
        input % Attribute("checked", Text("checked"), Null)
      }
      else
      {
        input
      }
    }

    bind("entry", xhtml,
         "selectLinks" ->
           <div id="selectLinks">
           {
             setChecked(<input onchange={SHtml.ajaxInvoke(showLinks(Positive))._2.cmd.toJsCmd} id="showPositiveLinks" type="radio" name="selectLinks" />, Positive) ++
             <label for="showPositiveLinks">Positive</label> ++
             setChecked(<input onchange={SHtml.ajaxInvoke(showLinks(Negative))._2.cmd.toJsCmd} id="showNegativeLinks" type="radio" name="selectLinks" />, Negative) ++
             <label for="showNegativeLinks">Negative</label>
           }
           </div>,
         "importReferenceLinks" -> SHtml.ajaxButton("Import ReferenceType Links", ImportReferenceLinksDialog.openCmd _),
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Evaluation" target="_help">Help</a>,
         "scripts" -> Script(OnLoad(ImportReferenceLinksDialog.initCmd))
    )
 }

  private def showLinks(linkType : EvalLink.ReferenceType)() =
  {
    ShowLinks() = linkType
    JS.Empty
  }
}
