import models.WorkbenchConfig
import play.api.{Application, GlobalSettings}
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import java.util.logging.{ConsoleHandler, SimpleFormatter, FileHandler}
import de.fuberlin.wiwiss.silk.workspace.FileUser
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import play.api.Play
import play.api.Play.current
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    
    // ensure user.home is set to $ELDS_HOME, if present
    val elds_home = System.getenv("ELDS_HOME")
    if (elds_home != null) {
      System.setProperty("user.home", elds_home)
    }
  
    // Configure logging
    val fileHandler = new FileHandler(app.getFile("/logs/engine.log").getAbsolutePath)
    fileHandler.setFormatter(new SimpleFormatter())
    java.util.logging.Logger.getLogger("").addHandler(fileHandler)

    val consoleHandler = new ConsoleHandler()
    java.util.logging.Logger.getLogger("").addHandler(consoleHandler)

    //Initialize user manager
    val user = new FileUser
    User.userManager = () => user

    //Load plugins
    Plugins.register()
    JenaPlugins.register()
  }
  
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(ex.getMessage))
  }
}

/**
 * Provides the global configuration.
 */
package object config {

  /* The baseUrl where the application is deployed */
  lazy val baseUrl = Play.configuration.getString("application.context").getOrElse("").stripSuffix("/")

  def workbench = WorkbenchConfig.get
}