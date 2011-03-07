package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.OnLoad
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.lift.util.JavaScriptUtils

/**
 * A dialog to edit a source task.
 */
class EditSourceTaskDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var uri = ""
    var graph = ""
    var retryCount = ""
    var retryPause = ""

    def submit() =
    {
      try
      {
        val sourceTask = User().sourceTask

        val params = Map("endpointURI" -> uri, "graph" -> graph, "retryCount" -> retryCount, "retryPause" -> retryPause)
        val source = Source(sourceTask.name, DataSource("sparqlEndpoint", params))
        val updatedSourceTask = SourceTask(source)

        User().project.sourceModule.update(updatedSourceTask)
        User().closeTask()

        EditSourceTaskDialog.closeCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "uri" -> SHtml.text(uri, uri = _, "id" -> "textboxUri", "size" -> "60"),
         "graph" -> SHtml.text(graph, graph = _, "id" -> "textboxGraph", "size" -> "60"),
         "retryCount" -> SHtml.text(retryCount, retryCount = _, "id" -> "textboxRetryCount", "size" -> "60"),
         "retryPause" -> SHtml.text(retryPause, retryPause = _, "id" -> "textboxRetryPause", "size" -> "60"),
         "submit" -> SHtml.ajaxSubmit("Edit", submit _))
    )
  }
}

object EditSourceTaskDialog
{
  def initCmd = OnLoad(JsRaw("$('#editSourceTaskDialog').dialog({ autoOpen: false, width: 700, modal: true, close : closeTask })").cmd)

  def openCmd =
  {
    //Retrieve source parameters
    val (uri, graph, retryCount, retryPause) = User().sourceTask.source.dataSource match
    {
      case DataSource(id, params) =>
      {
        (params("endpointURI"), params.get("graph").getOrElse(""), params.get("retryCount").getOrElse("3"), params.get("retryPause").getOrElse("1000"))
      }
    }

    //Fill textboxes with parameters and open the dialog
    JsRaw("$('#textboxUri').attr('value', '" + uri + "');").cmd &
    JsRaw("$('#textboxGraph').attr('value', '" + graph + "');").cmd &
    JsRaw("$('#textboxRetryCount').attr('value', '" + retryCount + "');").cmd &
    JsRaw("$('#textboxRetryPause').attr('value', '" + retryPause + "');").cmd &
    JsRaw("$('#editSourceTaskDialog').dialog('open');").cmd
  }

  def closeCmd =
  {
    JsRaw("$('#editSourceTaskDialog').dialog('close');").cmd &
    Workspace.updateCmd
  }
}