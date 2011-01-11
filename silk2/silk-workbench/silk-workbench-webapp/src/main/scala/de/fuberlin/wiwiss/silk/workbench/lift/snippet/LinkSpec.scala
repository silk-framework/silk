package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.project.Project
import de.fuberlin.wiwiss.silk.config.{ConfigWriter, ConfigReader}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.Call
import java.io.ByteArrayInputStream
import xml.{XML, NodeSeq}
import net.liftweb.http.{S, SHtml}

class LinkSpec
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    /**
     * Updates the Link Specification
     */
    def updateLinkSpec(linkSpecStr : String) =
    {
      try
      {
        val config = Project().config
        val sourceMap = config.sources.map(source => (source.id, source)).toMap

        val linkSpecXml = XML.loadString(linkSpecStr)
        val linkSpec = ConfigReader.readLinkSpecification(linkSpecXml, config.prefixes, sourceMap)

        Project.updateLinkSpec(linkSpec)
        JsRaw("alert('Updated Link Specification')").cmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('Error updating Link Specification. Details: " + ex.getMessage.encJs + "')").cmd
      }
    }

    //Serialize the link condition to a JavaScript string
    //TODO remove last replace?
    val linkSpecStr = ConfigWriter.serializeLinkSpec(Project().linkSpec).toString.replace("\n", " ").replace(" function=", " transformfunction=")

    val linkSpecVar = "var linkSpec = '" + linkSpecStr + "';"

    bind("entry", xhtml,
         "update" -> SHtml.ajaxButton("Update", () => SHtml.ajaxCall(Call("serializeLinkSpec"), updateLinkSpec)._2.cmd),
         "download" -> SHtml.submit("Download", () => S.redirectTo("config")),
         "linkSpec" -> Script(JsRaw(linkSpecVar).cmd))
  }
}
