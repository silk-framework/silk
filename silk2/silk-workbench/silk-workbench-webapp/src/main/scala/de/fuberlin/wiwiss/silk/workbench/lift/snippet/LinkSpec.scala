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
    def updateLinkSpec(linkConditionStr : String) =
    {
      try
      {
        val linkConditionXml = XML.loadString(linkConditionStr)
        val linkCondition = ConfigReader.readLinkCondition(linkConditionXml, Project().config.prefixes)
        val linkSpecification = Project().linkSpec.copy(condition = linkCondition)
        Project.updateLinkSpec(linkSpecification)
        JsRaw("alert('Updated Link Specification')").cmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('Error updating Link Specification. Details: " + ex.getMessage.encJs + "')").cmd
      }
    }

    //Serialize the link condition to a JavaScript string
    val linkConditionStr = ConfigWriter.serializeLinkCondition(Project().linkSpec.condition).toString.replace("\n", " ").replace(" function=", " transformfunction=")
    // val linkConditionStr = ConfigWriter.serializeLinkCondition(Project().linkSpec.condition).toString.replace("\n", " ")
    val linkConditionVar = "var linkCondition = '" + linkConditionStr + "';"

    bind("entry", xhtml,
         "update" -> SHtml.ajaxButton("Update", () => SHtml.ajaxCall(Call("serializeLinkCondition"), updateLinkSpec)._2.cmd),
         "download" -> SHtml.submit("Download", () => S.redirectTo("config")),
         "linkSpec" -> Script(JsRaw(linkConditionVar).cmd))
  }
}
