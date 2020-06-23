import org.silkframework.config.DefaultConfig
import play.api.{Configuration, Play}

/**
 * Provides the global configuration.
 */
package object config {

  /* The baseUrl where the application is deployed */
  lazy val baseUrl = {
    val config = Configuration(DefaultConfig.instance())
    val appContext = config.getOptional[String]("application.context")
    val playContext = config.getOptional[String]("play.http.context")
    appContext.getOrElse(playContext.getOrElse("")).stripSuffix("/")
  }

  def workbench = WorkbenchConfig.get
}
