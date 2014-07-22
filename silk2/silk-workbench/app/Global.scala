import java.io.File
import java.util.logging.{ConsoleHandler, FileHandler, SimpleFormatter}
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.workspace.{FileUser, User}
import play.api.Play.current
import play.api.mvc.{Handler, RequestHeader}
import play.api.mvc.Results._
import play.api.{Application, GlobalSettings, Play}
import play.core.Router.Routes
import plugins.WorkbenchPlugins

import scala.concurrent.Future

object Global extends GlobalSettings {

  private var pluginRoutes = Map[String, Routes]()

  override def beforeStart(app: Application) {
    // Configure logging
    configureLogging(app)

    // Initialize user manager
    val user = new FileUser
    User.userManager = () => user

    // Load Silk plugins
    Plugins.register()

    // Load Workbench plugins
    WorkbenchPlugins.register(DatasetPlugin())
    WorkbenchPlugins.register(TransformPlugin())
    WorkbenchPlugins.register(LinkingPlugin())
    //WorkbenchPlugins.register(WorkflowPlugin())

    pluginRoutes = WorkbenchPlugins().map(_.routes).reduce(_ ++ _)
    for((prefix, routes) <- pluginRoutes)
      routes.setPrefix(Routes.prefix + prefix + "/")
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val prefix = request.path.stripPrefix(Routes.prefix).takeWhile(_ != '/')
    pluginRoutes.get(prefix) match {
      case Some(routes) => routes.handlerFor(request)
      case None => super.onRouteRequest(request)
    }
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(ex.getMessage))
  }

  private def configureLogging(app: Application) {
    // If the ELDS_HOME variable is set, Silk is run inside the eccenca Linked Data Suite
    val elds_home = System.getenv("ELDS_HOME")

    // Configure workbench logging
    // The 'workbench.logDir' variable will be read in application.logger.xml
    if (elds_home != null)
      System.setProperty("workbench.logDir", elds_home + "/var/log/data_integration")
    else
      System.setProperty("workbench.logDir", Play.configuration.getString("application.home") + "logs")

    // Configure engine logging
    val logFile =
      if (elds_home != null)
        new File(elds_home + "/var/log/data_integration/engine.log")
      else if(app.getFile("/logs/").exists)
        app.getFile("/logs/engine.log")
      else
        app.getFile("../logs/engine.log")

    logFile.getParentFile.mkdirs()
    val fileHandler = new FileHandler(logFile.getAbsolutePath)
    fileHandler.setFormatter(new SimpleFormatter())
    java.util.logging.Logger.getLogger("").addHandler(fileHandler)

    val consoleHandler = new ConsoleHandler()
    java.util.logging.Logger.getLogger("").addHandler(consoleHandler)
  }
}