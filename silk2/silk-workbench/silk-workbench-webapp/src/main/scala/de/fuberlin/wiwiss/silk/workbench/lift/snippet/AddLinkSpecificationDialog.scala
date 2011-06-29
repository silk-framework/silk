package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.{User}
import xml.{XML, NodeSeq}
import java.io.ByteArrayInputStream
import net.liftweb.http.{SHtml, FileParamHolder}
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import net.liftweb.http.js.JsCmd
import de.fuberlin.wiwiss.silk.workbench.workspace.io.{SilkConfigImporter, ProjectImporter}
import de.fuberlin.wiwiss.silk.config.SilkConfig

/**
 * Dialog to add link specifications to a project.
 */
class AddLinkSpecificationDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var fileHolder : FileParamHolder = null

    def submit()
    {
      fileHolder match
      {
        case FileParamHolder(_, mime, _, data) =>
        {
          val config = SilkConfig.load(new ByteArrayInputStream(data))

          SilkConfigImporter(config, User().project)
        }
        case _ =>
      }
    }

    bind("entry", xhtml,
         "file" -> SHtml.fileUpload(fileHolder = _),
         "submit" -> SHtml.submit("Add", submit, "style" -> "float:right;"))
  }
}

object AddLinkSpecificationDialog
{
  def initCmd = OnLoad(JsRaw("$('#addLinkSpecificationDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#addLinkSpecificationDialog').dialog('open')").cmd
}