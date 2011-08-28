package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.io.ProjectImporter
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.{XML, NodeSeq}
import java.io.ByteArrayInputStream
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.util.Helpers._
import net.liftweb.http.{S, SHtml, FileParamHolder}

/**
 * Dialog to import projects into the workspace.
 */
class ImportProjectDialog {
  def render(xhtml: NodeSeq): NodeSeq = {
    var name = "New_Project"
    var fileHolder: FileParamHolder = null

    def submit() {
      try {
        fileHolder match {
          case FileParamHolder(_, mime, _, data) => {
            val project = User().workspace.createProject(name)
            val projectString = new String(data)

            try {
              if(projectString.contains("<Silk>")) {
                throw new Exception("Link Specification instead of project supplied. Create a new project and add the link specification.")
              }

              ProjectImporter(project, XML.loadString(projectString))
            }
            catch {
              case ex: Exception => {
                User().workspace.removeProject(name)
                throw ex
              }
            }
          }
          case _ =>
        }
      } catch {
        case ex: Exception => S.warning("Error importing project: " + ex.getMessage)
      }
    }

    bind("entry", xhtml,
      "name" -> SHtml.text(name, name = _, "size" -> "20", "title" -> "Project name"),
      "file" -> SHtml.fileUpload(fileHolder = _),
      "submit" -> SHtml.submit("Import", submit, "style" -> "float:right;"))
  }
}

object ImportProjectDialog {
  def initCmd = OnLoad(JsRaw("$('#importProjectDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#importProjectDialog').dialog('open')").cmd
}