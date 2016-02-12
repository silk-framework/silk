import org.silkframework.runtime.activity.Activity
import play.api.Application
import plugins.WorkbenchPlugins

object Global extends WorkbenchGlobal {

  override def beforeStart(app: Application) {
    // Use Play execution context for running activities
    Activity.executionContext = play.api.libs.concurrent.Execution.defaultContext

    // Load Workbench plugins
    WorkbenchPlugins.register(DatasetPlugin())
    WorkbenchPlugins.register(TransformPlugin())
    WorkbenchPlugins.register(LinkingPlugin())
    WorkbenchPlugins.register(WorkflowPlugin())

    super.beforeStart(app)
  }
}