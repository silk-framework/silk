package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.js.JsCmds.{Script, OnLoad, SetHtml}
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.lift.util.Widgets
import de.fuberlin.wiwiss.silk.workbench.lift.util.JavaScriptUtils.PeriodicUpdate
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvaluationServer

class Evaluation
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "control" -> Widgets.taskControl(EvaluationServer.evaluationTask))
  }

  def content(xhtml : NodeSeq) : NodeSeq =
  {
    def createTable() =
    {
      <table border="1">
        <tr>
          <th>Source</th>
          <th>Target</th>
          <th>Confidence</th>
          <th>Correct?</th>
        </tr>
        {
          for((link, correct) <- EvaluationServer.links) yield
          {
            <tr>
              <td><a href={link.sourceUri}>{link.sourceUri}</a></td>
              <td><a href={link.targetUri}>{link.targetUri}</a></td>
              <td>{link.confidence}</td>
              <td>{correct}</td>
            </tr>
          }
        }
      </table>
    }

    //Updates the table message
    def update() = SetHtml("alignemtTable", createTable())

    bind("entry", xhtml,
         "table" ->
           <p>
             <div id="alignemtTable">Waiting...</div>
             <div>{Script(OnLoad(PeriodicUpdate(update, 300000)))}</div>
           </p>)
  }
}
