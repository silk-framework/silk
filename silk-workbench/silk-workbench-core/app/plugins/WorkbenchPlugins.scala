package plugins

/**
 * Holds all available Workbench plugins.
 */
object WorkbenchPlugins {

  private var plugins = Seq[WorkbenchPlugin]()

  def register(plugin: WorkbenchPlugin) {
    plugins = plugins :+ plugin
  }

  def apply() = {
    plugins
  }

  def findTaskActions(taskData: AnyRef) = {
    plugins.flatMap(_.tasks).find(_.isCompatible(taskData)).get
  }
}
