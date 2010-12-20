package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.config.ConfigReader
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import java.net.URI
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workbench.project.{Project, Description}
import net.liftweb.http.{S, SHtml, FileParamHolder}
import java.io.{File, ByteArrayInputStream}
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.{SetHtml, Script, OnLoad}

class ProjectMenu
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    if(Project.isOpen)
    {
      chooseTemplate("choose", "projectOpen", xhtml)
    }
    else
    {
      chooseTemplate("choose", "projectClosed", xhtml)
    }
  }

  def menu(xhtml : NodeSeq) : NodeSeq =
  {
      def save()
      {
        S.redirectTo("project.silk")
      }

      def close()
      {
        Project.close()
      }

      bind("entry", xhtml,
           "save" -> SHtml.submit("Save", save),
           "close" -> SHtml.submit("Close", close))
  }

  def startMenu(xhtml : NodeSeq) : NodeSeq =
  {
    def showCreateDialog() =
    {
      JsRaw("$('#createProjectDialog').dialog('open')").cmd
    }

    def showOpenDialog() =
    {
      JsRaw("$('#openProjectDialog').dialog('open')").cmd
    }

    val initCreateDialog = Script(OnLoad(JsRaw("$('#createProjectDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd))
    val initOpenDialog = Script(OnLoad(JsRaw("$('#openProjectDialog').dialog({ autoOpen: false, modal: true })").cmd))

    bind("entry", xhtml,
         "new" -> (initCreateDialog ++ SHtml.ajaxButton("New", showCreateDialog _)),
         "open" -> (initOpenDialog ++ SHtml.ajaxButton("Open", showOpenDialog _)))
  }

  def createProjectDialog(xhtml : NodeSeq) : NodeSeq =
  {
    var sourceEndpointUri = "http://www4.wiwiss.fu-berlin.de/sider/sparql"
    var targetEndpointUri = "http://www4.wiwiss.fu-berlin.de/drugbank/sparql"
    var sourceRestriction = "?a rdf:type sider:drugs"
    var targetRestriction = "?b rdf:type drugbank:drugs"

    def run()
    {
      val sourceDataset = new Description(new RemoteSparqlEndpoint(new URI(sourceEndpointUri)), sourceRestriction)
      val targetDataset = new Description(new RemoteSparqlEndpoint(new URI(targetEndpointUri)), targetRestriction)

      Project.create(new SourceTargetPair(sourceDataset, targetDataset))

      S.redirectTo("linkSpec")
    }

    bind("entry", xhtml,
         "sourceEndpoint" -> SHtml.text(sourceEndpointUri, sourceEndpointUri = _, "size" -> "60"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60"),
         "targetEndpoint" -> SHtml.text(targetEndpointUri, targetEndpointUri = _, "size" -> "60"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60"),
         "submit" -> SHtml.submit("Create", run))
  }

  def openProjectDialog(xhtml : NodeSeq) : NodeSeq =
  {
    var fileHolder : FileParamHolder = null

    def submit()
    {
      fileHolder match
      {
        case FileParamHolder(_, _, _, data) =>
        {
          Project.open(new ByteArrayInputStream(data))
        }
        case _ =>
      }
    }


    bind("entry", xhtml,
         "file" -> SHtml.fileUpload(fileHolder = _),
         "submit" -> SHtml.submit("Open", submit, "style" -> "float:right;"))
  }

  def editPrefixes(f : () => Map[String, String]) : NodeSeq =
  {
    //var prefixes = Map("dbpedia" -> "http://dbpedia.org/resource/")

    var prefixes = List[(String, String)]()

    def update(str : String) =
    {
      println("VVV" + str)
      JsRaw("").cmd
    }

    def commit() =
    {
      SHtml.ajaxCall(JsRaw("$('#prefixTable tr td input').toArray().map(function (a) { return a.value; })"), update _)._2.cmd
    }

    def addRow() =
    {
      JsRaw("$('#prefixTable').append(\"<tr><td><input type='text' /></td></tr>\");").cmd
    }

    <table id="prefixTable">
      <tr>
        <th>Prefix</th>
        <th>Namespace</th>
      </tr>
      {
        for((prefix, namespace) <- prefixes) yield
        {
          <tr>
            <td>{prefix}</td>
            <td>{namespace}</td>
          </tr>
        }
      }
      <tr>
        <td>{SHtml.ajaxButton("commit", commit _)}</td>
        <td>{SHtml.ajaxButton("add", addRow _)}</td>
      </tr>
    </table>
  }
}
