import org.silkframework.runtime.activity.Activity
import play.api.Application
import plugins.WorkbenchPlugins

object Global extends WorkbenchGlobal {

  override def beforeStart(app: Application) {
    // Load Workbench plugins
    WorkbenchPlugins.register(WorkbenchDatasetPlugin())
    WorkbenchPlugins.register(TransformPlugin())
    WorkbenchPlugins.register(LinkingPlugin())
    WorkbenchPlugins.register(WorkflowPlugin())

    super.beforeStart(app)
  }
}
