package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * A dialog to create new datasources.
 */
class CreateSourceTaskDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var name = ""
    var uri = ""
    var graph = ""
    var retryCount = "3"
    var retryPause = "1000"

    def submit() =
    {
      try
      {
        val params = Map("endpointURI" -> uri, "graph" -> graph, "retryCount" -> retryCount, "retryPause" -> retryPause)
        val source = Source(name, DataSource("sparqlEndpoint", params))
        val sourceTask = SourceTask(source)

        User().project.sourceModule.update(sourceTask)

        JsRaw("$('#createSourceTaskDialog').dialog('close');").cmd & Workspace.updateWorkspaceCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "size" -> "60"),
         "uri" -> SHtml.text(uri, uri = _, "size" -> "60"),
         "graph" -> SHtml.text(graph, graph = _, "size" -> "60"),
         "retryCount" -> SHtml.text(retryCount, retryCount = _, "size" -> "60"),
         "retryPause" -> SHtml.text(retryPause, retryPause = _, "size" -> "60"),
         "submit" -> SHtml.ajaxSubmit("Create", submit _))
    )
  }
}