package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.{PrettyPrinter, NodeSeq}
import net.liftweb.http.{S, SHtml}
import de.fuberlin.wiwiss.silk.workbench.project.Project
import de.fuberlin.wiwiss.silk.config.{ConfigWriter, ConfigReader}
import java.io.ByteArrayInputStream
import net.liftweb.util.Helpers._

class LinkSpec
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var configStr = new PrettyPrinter(120, 2).format(ConfigWriter.serializeConfig(Project().config))

    def apply()
    {
      val config = ConfigReader.read(new ByteArrayInputStream(configStr.getBytes))
      Project.updateConfig(config)
      S.redirectTo("alignment")
    }

    bind("entry", xhtml,
         "properties" -> properties(),
         "linkSpec" -> SHtml.textarea(configStr, configStr = _, "rows" -> "30", "cols" -> "100"),
         "submit" -> SHtml.submit("Apply", apply))
  }

  private def properties() : NodeSeq =
  {
    <ul>{
      for(path <- Project().cache.instanceSpecs.source.paths) yield
      {
        <li>{path.toString}</li>
      }
    }</ul>
  }
}
