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

        var params = Map("endpointURI" -> uri, "retryCount" -> retryCount, "retryPause" -> retryPause)
        if(graph != "")
        {
          params += ("graph" -> graph)
        }
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
         "uri" -> SHtml.text(uri, uri = _, "id" -> "textboxUri", "size" -> "60", "title" -> "The URI of the SPARQL endpoint"),
         "graph" -> SHtml.text(graph, graph = _, "id" -> "textboxGraph", "size" -> "60", "title" -> "Only retrieve instances from a specific graph"),
         "retryCount" -> SHtml.text(retryCount, retryCount = _, "id" -> "textboxRetryCount", "size" -> "60", "title" -> "To recover from intermittent SPARQL endpoint connection failures, the 'retryCount' parameter specifies the number of times to retry connecting. By default, 'retryCount' is set to 3"),
         "retryPause" -> SHtml.text(retryPause, retryPause = _, "id" -> "textboxRetryPause", "size" -> "60", "title" -> "To recover from intermittent SPARQL endpoint connection failures, the 'retryPause' parameter specifies how long to wait between retries. By default, 'retryPause' is set to 1000"),
         "submit" -> SHtml.ajaxSubmit("Save", submit _))
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
    JsRaw("$('#textboxUri').val('" + uri + "');").cmd &
    JsRaw("$('#textboxGraph').val('" + graph + "');").cmd &
    JsRaw("$('#textboxRetryCount').val('" + retryCount + "');").cmd &
    JsRaw("$('#textboxRetryPause').val('" + retryPause + "');").cmd &
    JsRaw("$('#editSourceTaskDialog').dialog('open');").cmd
  }

  def closeCmd =
  {
    JsRaw("$('#editSourceTaskDialog').dialog('close');").cmd &
    Workspace.updateCmd
  }
}