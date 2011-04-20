package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.util.Task.Finished
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvaluationServer
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.output.Link
import xml.{NodeSeq, Elem}
import de.fuberlin.wiwiss.silk.instance.Path

/**
* A widget which displays the generated links of the evaluation server.
*/
class EvaluationLinks extends CometActor with Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
{
  EvaluationServer.evaluationTask.subscribe(this)

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
        case Task.StatusChanged(_, _) => reRender()
        case Task.Finished(_, _) => reRender()
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
          for((link, correct) <- EvaluationServer.links.view(page * pageSize, (page + 1) * pageSize)) yield
          {
            renderLink(link, correct)
          }
        }
      </table>

    SetHtml("results", html)
  }

  private def renderLink(link : Link, correct : Boolean) =
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
