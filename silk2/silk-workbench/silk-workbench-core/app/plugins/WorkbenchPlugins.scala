package plugins

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask

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

  def findTaskActions(task: ModuleTask) = {
    plugins.flatMap(_.tasks).find(_.isCompatible(task)).get
  }
}
