package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.util.Task.Finished
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.output.Link
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script}

/**
* A widget which displays the generated links of the evaluation server.
*/
class EvaluationLinks extends CometActor with Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
{
  User().evaluationTask.subscribe(this)

  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  private val pageSize = 100

  override def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
  {
    if(status.isInstanceOf[Finished] || System.currentTimeMillis - lastUpdateTime > minUpdatePeriod )
    {
      status match
      {
        case Task.StatusChanged(_, _) => partialUpdate(updateLinks)
        case Task.Finished(_, _) => partialUpdate(updateLinks)
        case _ =>
      }

      lastUpdateTime = System.currentTimeMillis
    }
  }

  override def render =
  {
    val showLinksFunc = JsCmds.Function("showLinks", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showLinks(pageStr.toInt))._2.cmd)

    <p>
      { Script(OnLoad(updateLinks) & showLinksFunc) }
      { <div id="results" /> }
    </p>
  }

  private def updateLinks() : JsCmd =
  {
    JsRaw("initPagination(" + User().evaluationTask.links.size + ");" +
          "showLinks(current_page);").cmd
  }

  private def showLinks(page : Int) =
  {
    val html =
      <div>
        {
          for((link, correct) <- User().evaluationTask.links.view(page * pageSize, (page + 1) * pageSize)) yield
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
        <div class="link-header">
          <div id={getId(link, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e" onclick={"toggleLinkDetails('" + getId(link) + "');"} ></span></div>
          <div class="source-link"><a href={link.sourceUri} target="_blank">{link.sourceUri}</a></div>
          <div class="target-link"><a href={link.targetUri} target="_blank">{link.targetUri}</a></div>
          <div class="confidencebar">{link.confidence}</div>
        </div>
        <div class="link-details" id={getId(link, "details")}>
            <ul class="details-tree">
              { renderSimilarity(link.details.get) }
            </ul>
        </div>
    </div>
  }

  private def renderSimilarity(similarity : Link.Similarity) : NodeSeq = similarity match
  {
    case Link.AggregatorSimilarity(value, children) =>
    {
      <li>
        <span class="aggregation">Aggregation</span>
          <ul>
            { renderConfidence(value) }
            { children.map(renderSimilarity) }
          </ul>
      </li>
    }
    case Link.ComparisonSimilarity(value, input1, input2) =>
    {
      <li>
        <span class="comparison">Comparison</span>
          <ul>
            { renderConfidence(value) }
            { renderInputValue(input1) }
            { renderInputValue(input2) }
          </ul>
      </li>
    }
  }

  private def renderConfidence(value : Option[Double]) = value match
  {
    case Some(v) => <li><span class="confidence">Confidence:{ v.toString }</span></li>
    case None => NodeSeq.Empty
  }

  private def renderInputValue(input : Link.InputValue) = input match
  {
    case Link.InputValue(path, values) =>
    {
      <li><span class="input">{ path.toString + ": " + values.mkString(", ") }</span></li>
    }
  }

  private def getId(link : Link, prefix : String = "") =
  {
    prefix + link.hashCode
  }
}
