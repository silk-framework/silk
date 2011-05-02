package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JavaScriptUtils, Widgets}
import de.fuberlin.wiwiss.silk.workbench.lift.comet.ShowReferenceLinks

class Evaluate
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "control" -> Widgets.taskControl(User().evaluationTask, cancelable = true),
         "selectLinks" ->
           <div id="selectLinks">
           {
             <input onchange={SHtml.ajaxInvoke(showGeneratedLinks)._2.cmd.toJsCmd} id="showGeneratedLinks" type="radio" name="selectLinks" checked="checked"/> ++
             <label for="showGeneratedLinks">Generated Links</label> ++
             <input onchange={SHtml.ajaxInvoke(showReferenceLinks)._2.cmd.toJsCmd} id="showReferenceLinks" type="radio" name="selectLinks"/> ++
             <label for="showReferenceLinks">Reference Links</label>
           }
           </div>)
  }

  private def showReferenceLinks() =
  {
    ShowReferenceLinks() = true
    JavaScriptUtils.Empty
  }

  private def showGeneratedLinks() =
  {
    ShowReferenceLinks() = false
    JavaScriptUtils.Empty
  }
}
