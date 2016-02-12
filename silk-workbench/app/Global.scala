import java.io.File
import java.util.logging.{ConsoleHandler, FileHandler, SimpleFormatter}

import org.silkframework.config.Config
import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.{FileUser, User}
import play.api.Play.current
import play.api.{Application, Play}
import plugins.WorkbenchPlugins

object Global extends WorkbenchGlobal {

  // Forward the silk.home property, so it can be read in other configurations such as the logging configuration
  System.setProperty("silk.home", Config().getString("silk.home"))

  override def beforeStart(app: Application) {
    // Use Play execution context for running activities
    Activity.executionContext = play.api.libs.concurrent.Execution.defaultContext

    // Load Workbench plugins
    WorkbenchPlugins.register(DatasetPlugin())
    WorkbenchPlugins.register(TransformPlugin())
    WorkbenchPlugins.register(LinkingPlugin())
    // WorkbenchPlugins.register(WorkflowPlugin())

    super.beforeStart(app)
  }
}