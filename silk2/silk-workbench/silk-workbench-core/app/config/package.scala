import play.api.Play
import play.api.Play.current

/**
 * Provides the global configuration.
 */
package object config {

  /* The baseUrl where the application is deployed */
  lazy val baseUrl = Play.configuration.getString("application.context").getOrElse("").stripSuffix("/")

  def workbench = WorkbenchConfig.get
}