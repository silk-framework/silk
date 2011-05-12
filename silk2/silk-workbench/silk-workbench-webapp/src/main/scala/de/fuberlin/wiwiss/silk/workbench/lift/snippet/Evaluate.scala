package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JavaScriptUtils, Widgets}
import de.fuberlin.wiwiss.silk.workbench.evaluation._

class Evaluate
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "control" -> Widgets.taskControl(User().evaluationTask, cancelable = true),
         "selectLinks" ->
           <div id="selectLinks">
           {
             <input onchange={SHtml.ajaxInvoke(showLinks(GeneratedLinks))._2.cmd.toJsCmd} id="showGeneratedLinks" type="radio" name="selectLinks" checked="checked"/> ++
             <label for="showGeneratedLinks">Generated</label> ++
             <input onchange={SHtml.ajaxInvoke(showLinks(PositiveLinks))._2.cmd.toJsCmd} id="showPositiveLinks" type="radio" name="selectLinks"/> ++
             <label for="showPositiveLinks">Positive</label>
             <input onchange={SHtml.ajaxInvoke(showLinks(NegativeLinks))._2.cmd.toJsCmd} id="showNegativeLinks" type="radio" name="selectLinks"/> ++
             <label for="showNegativeLinks">Negative</label>
           }
           </div>,
         "importReferenceLinks" -> SHtml.ajaxButton("Import Reference Links", ImportReferenceLinksDialog.openCmd _),
         "scripts" -> Script(OnLoad(ImportReferenceLinksDialog.initCmd))
    )
 }

  private def showLinks(linkType : LinkType)() =
  {
    ShowLinks() = linkType
    JavaScriptUtils.Empty
  }
}
