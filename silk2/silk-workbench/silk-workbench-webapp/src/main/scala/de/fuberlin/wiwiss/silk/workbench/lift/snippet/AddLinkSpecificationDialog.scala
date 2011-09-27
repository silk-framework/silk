package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.NodeSeq
import java.io.ByteArrayInputStream
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.io.SilkConfigImporter
import de.fuberlin.wiwiss.silk.config.LinkingConfig
import net.liftweb.http.{S, SHtml, FileParamHolder}
import de.fuberlin.wiwiss.silk.util.ValidationException

/**
 * Dialog to add link specifications to a project.
 */
class AddLinkSpecificationDialog {
  def render(xhtml: NodeSeq): NodeSeq = {
    var fileHolder: FileParamHolder = null

    def submit() {
      try {
        fileHolder match {
          case FileParamHolder(_, mime, _, data) => {
            val config = LinkingConfig.load(new ByteArrayInputStream(data))

            SilkConfigImporter(config, User().project)
          }
          case _ =>
        }
      } catch {
        case ex: ValidationException => for(error <- ex.errors) S.warning(error.toString)
        case ex: Exception => S.warning("Error loading link specification: " + ex.getMessage)
      }
    }

    bind("entry", xhtml,
      "file" -> SHtml.fileUpload(fileHolder = _),
      "submit" -> SHtml.submit("Add", submit, "style" -> "float:right;"))
  }
}

object AddLinkSpecificationDialog {
  def initCmd = OnLoad(JsRaw("$('#addLinkSpecificationDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#addLinkSpecificationDialog').dialog('open')").cmd
}