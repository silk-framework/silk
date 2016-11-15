import play.api.Play
import play.api.Play.current

/**
 * Provides the global configuration.
 */
package object config {

  /* The baseUrl where the application is deployed */
  lazy val baseUrl = {
    val appContext = Play.configuration.getString("application.context")
    val playContext = Play.configuration.getString("play.http.context")
    appContext.getOrElse(playContext.getOrElse("")).stripSuffix("/")
  }

  def workbench = WorkbenchConfig.get
}
