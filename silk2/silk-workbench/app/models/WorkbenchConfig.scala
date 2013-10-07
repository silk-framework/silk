package models

import java.io.File
import scala.xml.{Elem, XML}

/**
 * Workbench configuration.
 *
 * @param title
 */
case class WorkbenchConfig(title: String = "Silk Workbench", logo: String = "/logo.png") {
}

object WorkbenchConfig {
  /**
   * Retrieves the Workbench configuration.
   */
  lazy val get = {
    val configFile = new File("config.xml")
    if(configFile.exists) {
      fromXml(XML.loadFile(configFile))
    } else {
      new WorkbenchConfig()
    }
  }

  /**
   * Loads the Workbench configuration from xml.
   */
  private def fromXml(xml: Elem) = {
    new WorkbenchConfig(
      title = (xml \ "Branding" \ "Title").text,
      logo  = (xml \ "Branding" \ "Logo").text
    )
  }
}
