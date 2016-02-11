import java.io.File
import java.util.logging.{ConsoleHandler, FileHandler, SimpleFormatter}

import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.{FileUser, User}
import play.api.Play.current
import play.api.{Application, Play}
import plugins.WorkbenchPlugins

object Global extends WorkbenchGlobal {

  override def beforeStart(app: Application) {
    // Use Play execution context for running activities
    Activity.executionContext = play.api.libs.concurrent.Execution.defaultContext

    // Configure logging
    configureLogging(app)

    // Load Workbench plugins
    WorkbenchPlugins.register(DatasetPlugin())
    WorkbenchPlugins.register(TransformPlugin())
    WorkbenchPlugins.register(LinkingPlugin())
    // WorkbenchPlugins.register(WorkflowPlugin())

    super.beforeStart(app)
  }

  private def configureLogging(app: Application) {
    // If the ELDS_HOME variable is set, Silk is run inside the eccenca Linked Data Suite
    val elds_home = System.getenv("ELDS_HOME")

    // Configure workbench logging
    // The 'workbench.logDir' variable will be read in application.logger.xml
    if (elds_home != null)
      System.setProperty("workbench.logDir", elds_home + "/var/log/dataintegration")
    else
      System.setProperty("workbench.logDir", Play.configuration.getString("application.home") + "logs")

    // Configure engine logging
    val logFile =
      if (elds_home != null)
        new File(elds_home + "/var/log/dataintegration/engine.log")
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