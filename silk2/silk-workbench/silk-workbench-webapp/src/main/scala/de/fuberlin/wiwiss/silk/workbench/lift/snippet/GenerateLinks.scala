package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, Widgets}
import net.liftweb.http.js.JsCmd

class GenerateLinks
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    val evaluationTask = User().evaluationTask

    def setOutput(enabled : Boolean) : JsCmd =
    {
      evaluationTask.outputEnabled = enabled
      JS.Empty
    }

    bind("entry", xhtml,
         "enableOutput" -> (SHtml.ajaxCheckbox(evaluationTask.outputEnabled, setOutput _, ("id" -> "enableOutput")) ++ <label for="enableOutput">Enable Output</label>),
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Evaluation" target="_help">Help</a>
    )
 }
}
