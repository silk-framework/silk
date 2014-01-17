package models

import java.io.{FileNotFoundException, File}
import scala.xml.{Elem, XML}
import play.api.Play
import play.api.Play.current
import models.WorkbenchConfig.Tabs

/**
 * Workbench configuration.
 *
 * @param title The application title.
 * @param logo The application logo. Must point to a file in the conf directory.
 * @param tabs The shown tabs.
 */
case class WorkbenchConfig(title: String = "Silk Workbench", logo: File = new File("logo.png"), tabs: Tabs = Tabs()) {
}

object WorkbenchConfig {
  /**
   * Retrieves the Workbench configuration.
   */
  lazy val get = {
    val config = Play.configuration

    WorkbenchConfig(
      title = config.getString("workbench.title").getOrElse("Silk Workbench"),
      logo = loadLogo(config.getString("workbench.logo").getOrElse("logo.png")),
      tabs = Tabs(
               config.getBoolean("workbench.tabs.editor").getOrElse(true),
               config.getBoolean("workbench.tabs.generateLinks").getOrElse(true),
               config.getBoolean("workbench.tabs.learn").getOrElse(true),
               config.getBoolean("workbench.tabs.referenceLinks").getOrElse(true),
               config.getBoolean("workbench.tabs.status").getOrElse(true)
             )
    )
  }

  /**
   * Loads the Workbench logo.
   */
  private def loadLogo(file: String) = {
    //Depending on the distribution method, the configuration directory may be located at different paths
    val paths = "conf/" + file :: "conf/_/" + file ::
      "../conf/" + file :: "../conf/_/" + file :: Nil

    //Trying all paths and using the first one that works
    val files = paths.map(Play.getFile)
    files.find(_.exists) match {
      case Some(f) => f
      case None =>
        throw new FileNotFoundException("Logo file not found. Tried paths: " + files.map(_.getAbsolutePath).mkString(", "))
    }
  }

  /**
   * Controls which tabs are shown.
   */
  case class Tabs(editor: Boolean = true,
                  generateLinks: Boolean = true,
                  learn: Boolean = true,
                  referenceLinks:Boolean = true,
                  status: Boolean = true)
}
