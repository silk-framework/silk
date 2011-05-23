package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader
import java.io.ByteArrayInputStream
import io.Source
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.{SHtml, S, FileParamHolder}
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad

class ImportReferenceLinksDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var fileHolder : FileParamHolder = null

    def submit()
    {
      fileHolder match
      {
        case FileParamHolder(_, _, fileName, data) =>
        {
          val alignment = fileName.split('.').last match
          {
            case "xml" => AlignmentReader.readAlignment(new ByteArrayInputStream(data))
            case "nt" => AlignmentReader.readNTriples(Source.fromBytes(data))
          }

          //If the alignment does not define any negative links -> generate some
          if(alignment.negative.isEmpty)
          {
            val updatedLinkingTask = User().linkingTask.copy(alignment = alignment.generateNegative)

            User().project.linkingModule.update(updatedLinkingTask)
            User().task = updatedLinkingTask
          }
          else
          {
            val updatedLinkingTask = User().linkingTask.copy(alignment = alignment)
            User().project.linkingModule.update(updatedLinkingTask)
            User().task = updatedLinkingTask
          }

          S.redirectTo("referenceLinks")
        }
        case _ =>
      }
    }

    bind("entry", xhtml,
         "file" -> SHtml.fileUpload(fileHolder = _),
         "submit" -> SHtml.submit("Open", submit, "style" -> "float:right;"))
  }
}

object ImportReferenceLinksDialog
{
  def initCmd = OnLoad(JsRaw("$('#importReferenceLinksDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#importReferenceLinksDialog').dialog('open')").cmd
}