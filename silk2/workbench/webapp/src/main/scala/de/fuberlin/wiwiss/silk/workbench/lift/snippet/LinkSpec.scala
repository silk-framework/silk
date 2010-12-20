package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.{PrettyPrinter, NodeSeq}
import net.liftweb.http.{S, SHtml}
import de.fuberlin.wiwiss.silk.workbench.project.Project
import de.fuberlin.wiwiss.silk.config.{ConfigWriter, ConfigReader}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import net.liftweb.util.Helpers._
import scala.xml.Utility.escape

class LinkSpec
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
//    def apply()
//    {
//      val config = ConfigReader.read(new ByteArrayInputStream(configStr.getBytes))
//      Project.updateConfig(config)
//      S.redirectTo("alignment")
//    }

    val linkConditionStr = ConfigWriter.serializeLinkCondition(Project().linkSpec.condition).toString.replace("\n", " ")
    val linkConditionVar = "var linkCondition = '" + linkConditionStr + "';"

    bind("entry", xhtml,
         "linkSpec" -> Script(JsRaw(linkConditionVar).cmd))
  }
}
