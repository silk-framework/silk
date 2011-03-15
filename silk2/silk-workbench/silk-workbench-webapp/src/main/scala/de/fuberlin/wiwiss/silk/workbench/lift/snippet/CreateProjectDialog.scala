package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._

/**
 * A dialog to create new projects.
 */
class CreateProjectDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var name = ""

    def submit() =
    {
      try
      {
        User().workspace.createProject(name)

        JsRaw("$('#createProjectDialog').dialog('close');").cmd & Workspace.updateCmd
      }
      catch
      {
        case ex : Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "id" -> "projectName", "size" -> "60","title" -> "Project name"),
         "submit" -> SHtml.ajaxSubmit("Create", submit)))
  }
}

object CreateProjectDialog
{
  def initCmd = OnLoad(JsRaw("$('#createProjectDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  def openCmd = JsRaw("$('#projectName').val('');").cmd & JsRaw("$('#createProjectDialog').dialog('open');").cmd
}