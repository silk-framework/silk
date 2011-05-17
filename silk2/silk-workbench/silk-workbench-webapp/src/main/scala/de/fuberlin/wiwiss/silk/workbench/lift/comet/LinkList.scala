package de.fuberlin.wiwiss.silk.workbench.lift.comet

import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.{Text, NodeSeq}

/**
* A widget which displays a list of links.
*/
trait LinkList extends CometActor
{
  /** The number of links shown on one page */
  private val pageSize = 100

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
            <div class="source-link"><span>Source</span></div>
            <div class="target-link"><span>Target</span></div>
            <div><span>Confidence</span></div>
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
    <div class="link">
      <div class="link-header" onclick={"toggleLinkDetails('" + getId(link) + "');"} onmouseover="$(this).addClass('link-over');" onmouseout="$(this).removeClass('link-over');">
        <div id={getId(link, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
        <div class="source-link"><a href={link.sourceUri} target="_blank">{link.sourceUri}</a></div>
        <div class="target-link"><a href={link.targetUri} target="_blank">{link.targetUri}</a></div>
        <div class="confidencebar"><div class="confidence">{"%.1f".format(link.confidence * 100)}%</div></div>
        <div class="link-buttons">
          <div id={getId(link, "unknownLink")} style={if(correct == 0) "display:block" else "display:none"}>
          {SHtml.a(() => declineLink(link), <img src="./static/img/decline.png" />)}
          {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm.png" />)}
          </div>
          <div id={getId(link, "confirmedLink")} style={if(correct == 1) "display:block" else "display:none"}>
          {SHtml.a(() => resetLink(link), <img src="./static/img/correct.png" />)}
          </div>
          <div id={getId(link, "declinedLink")} style={if(correct == -1) "display:block" else "display:none"}>
          {SHtml.a(() => resetLink(link), <img src="./static/img/uncorrect.png" />)}
          </div>
        </div>
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

  private def confirmLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive + link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "confirmedLink")) & JsHideId(getId(link, "unknownLink"))
  }

  private def declineLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(negative = alignment.negative + link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "declinedLink")) & JsHideId(getId(link, "unknownLink"))
  }

  private def resetLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "unknownLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink"))
  }

  private def getId(link : Link, prefix : String = "") =
  {
    prefix + link.hashCode
  }

  protected def linkCount : Int

  protected def links(from : Int, until : Int) : Traversable[(Link, Int)]
}
