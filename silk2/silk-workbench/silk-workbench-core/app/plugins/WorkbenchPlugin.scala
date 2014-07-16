package plugins

import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import scala.reflect.ClassTag

/**
 * Base trait for all Workbench plugins.
 */
trait WorkbenchPlugin {

  /**
   * The task types to be added to the Workspace.
   */
  def tasks: Seq[TaskActions[ModuleTask]]

  /**
   * Given a request context, lists the shown tabs.
   */
  def tabs(context: Context[ModuleTask]): Seq[Tab]

}

object WorkbenchPlugin {

  /**
   * A new task in the Workspace
   */
  abstract class TaskActions[+T <: ModuleTask : ClassTag] {

    /** The name of the task type */
    def name: String

    /** Path to the task icon */
    def icon: String

    /** The path to the dialog for creating a new task. */
    def createDialog(project: String): Option[String]

    /** The path to the dialog for editing an existing task. */
    def editDialog(project: String, task: String): Option[String]

    /** The path to redirect to when the task is opened. */
    def open(project: String, task: String): Option[String]

    /**  The path to delete the task by sending a DELETE HTTP request. */
    def delete(project: String, task: String): Option[String]

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    def properties(task: ModuleTask): Seq[(String, String)]

    /** Retrieves all tasks of this type from a project*/
    def projectTasks(project: Project) = project.tasks[T]
  }

  /**
   * A tab in the tabbar.
   *
   * @param title The title that is displayed on the tab.
   * @param path The target when the user clicks on the tab.
   */
  case class Tab(title: String, path: String)
}
