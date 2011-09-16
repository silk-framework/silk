package de.fuberlin.wiwiss.silk.workbench.lift.comet

import xml.NodeSeq
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.JE.JsRaw

/**
 * A widget which shows context-sensitive help to the current tab.
 * Each inheriting class should provide two functions:
 * renderOverview: A short overview of the current tab
 * renderActions: A list of recommended actions
 */
trait Help extends CometActor {
  /**
   * A short overview of the current tab.
   */
  protected def overview: NodeSeq

  /**
   * A list of recommended actions
   */
  protected def actions: NodeSeq = NodeSeq.Empty

  /**
   * Renders this widget.
   */
  override def render = {
   <div>
     { renderStatic }
     { renderDynamic }
   </div>
  }

  /**
   * Renders the static part of the help
   */
  private def renderStatic = {
    <b>Overview</b><br/> ++ overview
  }

  /**
   * Renders the dynamic part of the help.
   */
  private def renderDynamic = {
    val nodes = actions
    if(nodes.isEmpty)
      nodes
    else
      <br/><b>Next Steps</b><br/> ++ nodes
  }
}