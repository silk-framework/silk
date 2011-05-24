package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.io.{ProjectImporter}
import de.fuberlin.wiwiss.silk.workbench.workspace.{User}
import xml.{XML, NodeSeq}
import java.io.ByteArrayInputStream
import net.liftweb.http.{SHtml, FileParamHolder}
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import net.liftweb.http.js.JsCmd

/**
 * Dialog to import projects into the workspace.
 */
class ImportProjectDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var name = "New Project"
    var fileHolder : FileParamHolder = null

    def submit()
    {
      fileHolder match
      {
        case FileParamHolder(_, mime, _, data) =>
        {
          val project = User().workspace.createProject(name)

          try
          {
            ProjectImporter(project, XML.load(new ByteArrayInputStream(data)))
          }
          catch
          {
            case ex : Exception =>
            {
              User().workspace.removeProject(name)
              throw ex
            }
          }
        }
        case _ =>
      }
    }

      bind("entry", xhtml,
           "name" -> SHtml.text(name, name = _, "size" -> "20", "title" -> "Project name"),
           "file" -> SHtml.fileUpload(fileHolder = _),
           "submit" -> SHtml.submit("Import", submit, "style" -> "float:right;"))
  }
}

object ImportProjectDialog
{
  def initCmd = OnLoad(JsRaw("$('#importProjectDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#importProjectDialog').dialog('open')").cmd
}