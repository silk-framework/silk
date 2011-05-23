package de.fuberlin.wiwiss.silk.workbench.lift.comet

import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.{Text, NodeSeq}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JavaScriptUtils

/**
* A widget which displays a list of links.
*/
trait LinkList extends CometActor
{
  /** The number of links shown on one page */
  private val pageSize = 100

  protected val showStatus = true

  protected val showButtons = true

  protected def linkCount : Int

  protected def links(from : Int, until : Int) : Traversable[(Link, Int)]

  protected def renderStatus(link : Link, correct : Int) : NodeSeq = NodeSeq.Empty

  protected def renderButtons(link : Link, correct : Int) : NodeSeq = NodeSeq.Empty

  override def render =
  {
    val showLinksFunc = JsCmds.Function("showLinks", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showLinks(pageStr.toInt))._2.cmd)

    <p>
      { Script(OnLoad(updateLinks) & showLinksFunc) }
      { <div id="results" /> }
    </p>
  }

  protected def updateLinks() : JsCmd =
  {
    JsRaw("initPagination(" + linkCount + ");").cmd
  }

  private def showLinks(page : Int) =
  {
    val html =
      <div>
        <div class="link">
          <div class="link-header heading">
            <div class="link-source"><span>Source</span></div>
            <div class="link-target"><span>Target</span></div>
            <div class="link-confidence"><span>Confidence</span></div>
            { if(showStatus) <div class="link-status"><span>Status</span></div> else NodeSeq.Empty }
            { if(showButtons) <div class="link-buttons"></div> else NodeSeq.Empty }
          </div>
        </div>
        {
          for((link, correct) <- links(page * pageSize, (page + 1) * pageSize)) yield
          {
            renderLink(link, correct)
          }
        }
      </div>

    SetHtml("results", html) & JsRaw("initTrees();").cmd
  }

  /**
   * Renders a link.
   *
   * @param link The link to be rendered
   * @param correct 1 if this link is correct. -1 if it is wrong. 0 if unknown.
   */
  private def renderLink(link : Link, correct : Int) =
  {
    <div class="link" id={getId(link)} >
      <div class="link-header" onclick={"toggleLinkDetails('" + getId(link) + "');"} onmouseover="$(this).addClass('link-over');" onmouseout="$(this).removeClass('link-over');">
        <div id={getId(link, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
        <div class="link-source"><a href={link.sourceUri} target="_blank">{link.sourceUri}</a></div>
        <div class="link-target"><a href={link.targetUri} target="_blank">{link.targetUri}</a></div>
        <div class="confidencebar"><div class="confidence">{"%.1f".format(link.confidence * 100)}%</div></div>
        { if(showStatus) <div class="link-status">{ renderStatus(link, correct) }</div> else NodeSeq.Empty }
        { if(showButtons) <div class="link-buttons">{ renderButtons(link, correct) }</div> else NodeSeq.Empty }

      </div>
      <div class="link-details" id={getId(link, "details")}>
      { renderDetails(link.details) }
      </div>
      <div style="clear:both"></div>
    </div>
  }

  private def renderDetails(details : Option[Link.Similarity]) : NodeSeq =
  {
    details match
    {
      case Some(similarity) =>
      {
        <ul class="details-tree">
        { renderSimilarity(similarity) }
        </ul>
      }
      case None => Text("No details")
    }
  }

  private def renderSimilarity(similarity : Link.Similarity) : NodeSeq = similarity match
  {
    case Link.AggregatorSimilarity(value, children) =>
    {
      <li>
        <span class="aggregation">Aggregation</span>{ renderConfidence(value) }
          <ul>
            { children.map(renderSimilarity) }
          </ul>
      </li>
    }
    case Link.ComparisonSimilarity(value, input1, input2) =>
    {
      <li>
        <span class="comparison">Comparison</span>{ renderConfidence(value) }
          <ul>
            { renderInputValue(input1) }
            { renderInputValue(input2) }
          </ul>
      </li>
    }
  }
  private def renderConfidence(value : Option[Double]) = value match
  {
    case Some(v) => <div class="confidencebar"><div class="confidence">{"%.1f".format(v * 100)}%</div></div>
    case None => NodeSeq.Empty
  }

  private def renderInputValue(input : Link.InputValue) = input match
  {
    case Link.InputValue(path, values) =>
    {
      <li><span class="input">Input <span class="path">{path.toString}</span>{values.map(v => <span class="value">{v}</span>) }</span></li>
    }
  }

  protected def getId(link : Link, prefix : String = "") =
  {
    prefix + link.hashCode
  }
}
