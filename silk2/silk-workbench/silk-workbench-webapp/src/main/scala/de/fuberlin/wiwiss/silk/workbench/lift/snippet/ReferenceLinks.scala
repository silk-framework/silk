package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Positive, Negative}

class ReferenceLinks
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "selectLinks" ->
           <div id="selectLinks">
           {
             <input onchange={SHtml.ajaxInvoke(showLinks(Positive))._2.cmd.toJsCmd} id="showPositiveLinks" type="radio" name="selectLinks"/> ++
             <label for="showPositiveLinks">Positive</label>
             <input onchange={SHtml.ajaxInvoke(showLinks(Negative))._2.cmd.toJsCmd} id="showNegativeLinks" type="radio" name="selectLinks"/> ++
             <label for="showNegativeLinks">Negative</label>
           }
           </div>,
         "importReferenceLinks" -> SHtml.ajaxButton("Import Reference Links", ImportReferenceLinksDialog.openCmd _),
         "scripts" -> Script(OnLoad(ImportReferenceLinksDialog.initCmd))
    )
 }

  private def showLinks(linkType : EvalLink.Reference)() =
  {
    ShowLinks() = linkType
    JS.Empty
  }
}
