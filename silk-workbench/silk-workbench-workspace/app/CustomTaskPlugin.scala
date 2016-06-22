import org.silkframework.config.CustomTaskSpecification
import org.silkframework.runtime.plugin.PluginRegistry
import plugins.WorkbenchPlugin
import plugins.WorkbenchPlugin.TaskActions

/**
 * Adds custom tasks
 */
case class CustomTaskPlugin() extends WorkbenchPlugin {
  /**
   * The task types to be added to the Workspace.
   */
  override def tasks: Seq[TaskActions[_]] = Seq(CustomTasksActions)

  object CustomTasksActions extends TaskActions[CustomTaskSpecification] {

    /** The name of the task type */
    override def name: String = "Other"

    /** Path to the task icon */
    override def icon: String = "img/task.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/customTasks/newTaskDialog/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"workspace/customTasks/editTaskDialog/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      None

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"workspace/projects/$project/customTasks/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(taskData: Any): Seq[(String, String)] = {
      val (pluginType, params) = PluginRegistry.reflect(taskData.asInstanceOf[CustomTaskSpecification].plugin)
      params.toSeq
    }
  }
}
