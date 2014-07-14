package plugins

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask

/**
 * Base trait for all Workbench plugins.
 */
trait WorkbenchPlugin {

  /**
   * Given a request context, lists the shown tabs.
   */
  def tabs(context: Context[ModuleTask]): Seq[Tab]

  /**
   * A tab in the tabbar.
   *
   * @param title The title that is displayed on the tab.
   * @param path The target when the user clicks on the tab.
   */
  case class Tab(title: String, path: String)
}
