package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.BindHelpers._
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS._

/**
 * Renders the editor toolbar.
 */
class EditorToolbar {
  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml,
         "export" -> SHtml.ajaxButton("Export as Silk-LS", () => Redirect("config.xml")),
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Link_Specification_Editor" target="_help">Help</a>)
  }
}