package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.util.Task.Finished
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvaluationServer
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.http.js.JE.JsRaw

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
            <tr>
              <td>{link.sourceUri}</td>
              <td>{link.targetUri}</td>
              <td>{link.confidence}</td>
              <td>{correct}</td>
            </tr>
          }
        }
      </table>

    SetHtml("results", html)
  }
}
