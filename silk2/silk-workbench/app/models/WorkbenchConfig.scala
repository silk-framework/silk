package models

import java.io.{FileNotFoundException, File}
import scala.xml.{Elem, XML}
import play.api.Play
import play.api.Play.current

/**
 * Workbench configuration.
 *
 * @param title The application title.
 * @param logo The application logo. Must point to a file in the conf directory.
 */
case class WorkbenchConfig(title: String = "Silk Workbench", logo: File = new File("logo.png")) {
}

object WorkbenchConfig {
  /**
   * Retrieves the Workbench configuration.
   */
  lazy val get = {
    val config = Play.configuration

    WorkbenchConfig(
      title = config.getString("workbench.title").getOrElse("Silk Workbench"),
      logo = {
        // Reading the file name of the logo
        val logoFile = config.getString("workbench.logo").getOrElse("logo.png")

        //Depending on the distribution method, the configuration directory may be located at different paths
        val paths = "conf/" + logoFile :: "conf/_/" + logoFile ::
                    "../conf/" + logoFile :: "../conf/_/" + logoFile :: Nil

        //Trying all paths and using the first one that works
        val files = paths.map(Play.getFile)
        files.find(_.exists) match {
          case Some(f) => f
          case None => {
            throw new FileNotFoundException("Logo file not found. Tried paths: " + files.map(_.getAbsolutePath).mkString(", "))
          }
        }
      }
    )
  }
}
