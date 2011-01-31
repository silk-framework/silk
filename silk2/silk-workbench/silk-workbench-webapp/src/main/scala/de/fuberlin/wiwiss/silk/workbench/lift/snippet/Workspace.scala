package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.http.js.JE.{JsRaw, JsVar}
import net.liftweb.json.JsonDSL._
import net.liftweb.http.js.JsCmds.{Script, JsReturn}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.{JE, JsCmds}
import net.liftweb.json.JsonAST

class Workspace
{
  def content(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "workspaceVar" -> Script(JsRaw("var workspaceVar = " + pretty(JsonAST.render(generateJson)) + ";").cmd))
  }

  private def generateJson : JValue =
  {
    val project = User().project

    val sources : JArray = for(task <- project.sourceModule.tasks.toSeq) yield
    {
      ("name" -> task.name) ~
      ("url" -> task.source.dataSource.toString)
    }

    val linkingTasks : JArray = for(task <- project.linkingModule.tasks.toSeq) yield
    {
      ("name" -> task.name) ~
      ("source" -> task.linkSpec.datasets.source.sourceId) ~
      ("target" -> task.linkSpec.datasets.target.sourceId) ~
      ("sourceDataset" -> task.linkSpec.datasets.source.restriction) ~
      ("targetDataset" -> task.linkSpec.datasets.target.restriction)
    }

    ("workspace" ->
      ("project" ->
        (
          ("name" -> "project") ~
          ("dataSource" -> sources) ~
          ("linkingTask" -> linkingTasks)
        )
      )
    )
  }
}
