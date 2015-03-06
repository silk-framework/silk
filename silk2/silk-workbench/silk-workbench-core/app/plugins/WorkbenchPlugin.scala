package plugins

import de.fuberlin.wiwiss.silk.workspace.Project
import play.core.Router.Routes
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import scala.reflect.ClassTag

/**
 * Base trait for all Workbench plugins.
 */
trait WorkbenchPlugin {

  def routes: Map[String, Routes] = Map.empty

  /**
   * The task types to be added to the Workspace.
   */
  def tasks: Seq[TaskActions[_]] = Seq.empty

  /**
   * Given a request context, lists the shown tabs.
   */
  def tabs(context: Context[_]): Seq[Tab] = Seq.empty

}

object WorkbenchPlugin {

  /**
   * A new task in the Workspace
   */
  abstract class TaskActions[T : ClassTag] {

    /** The name of the task type */
    def name: String

    /** Path to the task icon */
    def icon: String

    /** The path to the dialog for creating a new task. */
    def createDialog(project: String): Option[String]

    /** The path to the dialog for editing an existing task. */
    def propertiesDialog(project: String, task: String): Option[String]

    /** The path to redirect to when the task is opened. */
    def open(project: String, task: String): Option[String]

    /**  The path to delete the task by sending a DELETE HTTP request. */
    def delete(project: String, task: String): Option[String]

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    def properties(taskData: Any): Seq[(String, String)]

    /** Retrieves all tasks of this type from a project*/
    def projectTasks(project: Project) = project.tasks[T]

    def isCompatible(taskData: AnyRef) = taskData.getClass == implicitly[ClassTag[T]].runtimeClass
  }

  /**
   * A tab in the tabbar.
   *
   * @param title The title that is displayed on the tab.
   * @param path The target when the user clicks on the tab.
   */
  case class Tab(title: String, path: String)
}
