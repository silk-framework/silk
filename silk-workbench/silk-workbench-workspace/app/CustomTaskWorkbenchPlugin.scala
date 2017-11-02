
import org.silkframework.config.{CustomTask, Prefixes, TaskSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import plugins.WorkbenchPlugin
import plugins.WorkbenchPlugin.TaskActions
import controllers.workspace.routes.Assets

/**
 * Adds custom tasks
 */
case class CustomTaskWorkbenchPlugin() extends WorkbenchPlugin {
  /**
   * The task types to be added to the Workspace.
   */
  override def tasks: Seq[TaskActions[_ <: TaskSpec]] = Seq(CustomTasksActions)

  object CustomTasksActions extends TaskActions[CustomTask] {

    /** The name of the task type */
    override def name: String = "Other"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/task.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/customTasks/newTaskDialog/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"workspace/customTasks/editTaskDialog/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      None

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(taskData: Any)(implicit prefixes: Prefixes): Seq[(String, String)] = {
      val (pluginType, params) = PluginRegistry.reflect(taskData.asInstanceOf[CustomTask])
      params.toSeq
    }
  }
}
