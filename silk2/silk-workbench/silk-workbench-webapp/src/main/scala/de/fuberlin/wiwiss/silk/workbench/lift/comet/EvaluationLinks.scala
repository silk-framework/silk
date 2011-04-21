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
    <p>
      { Script(JsCmds.Function("showLinks", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showLinks(pageStr.toInt))._2.cmd )) }
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
      <table border="1">
        <tr>
          <th>Source</th>
          <th>Target</th>
          <th>Confidence</th>
          <th>Correct?</th>
        </tr>
        {
          for((link, correct) <- User().evaluationTask.links.view(page * pageSize, (page + 1) * pageSize)) yield
          {
            renderLink(link, correct)
          }
        }
      </table>

    SetHtml("results", html)
  }

  /**
   * Renders a link.
   *
   * @param link The link to be rendered
   * @param correct 1 if this link is correct. -1 if it is wrong. 0 if unknown.
   */
  private def renderLink(link : Link, correct : Int) =
  {
    <tr>
      <td>{link.sourceUri}</td>
      <td>{link.targetUri}</td>
      <td>{link.confidence}</td>
      <td>{correct}</td>
    </tr>
    <tr>
      <td colspan="4">
        <div>
          { renderSimilarity(link.details.get) }
        </div>
      </td>
    </tr>
  }

  private def renderSimilarity(similarity : Link.Similarity) : NodeSeq = similarity match
  {
    case Link.AggregatorSimilarity(value, children) =>
    {
      <div id="aggregation">
        { renderConfidence(value) }
        { children.map(renderSimilarity) }
      </div>
    }
    case Link.ComparisonSimilarity(value, input1, input2) =>
    {
      <div id="comparison">
        { renderConfidence(value) }
        { renderInputValue(input1) }
        { renderInputValue(input2) }
      </div>
    }
  }

  private def renderConfidence(value : Option[Double]) = value match
  {
    case Some(v) => <div id="confidence">{ v.toString }</div>
    case None => NodeSeq.Empty
  }

  private def renderInputValue(input : Link.InputValue) = input match
  {
    case Link.InputValue(path, values) =>
    {
      <div id="input">{ path.toString + ": " + values.mkString(", ") }</div>
    }
  }

}
