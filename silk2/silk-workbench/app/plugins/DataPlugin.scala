package plugins

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.output.DataWriter
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.output.OutputTask
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceTask
import plugins.WorkbenchPlugin.{Tab, TaskActions}

class DataPlugin extends WorkbenchPlugin {
  /**
   * The task types to be added to the Workspace.
   */
  override def tasks: Seq[TaskActions[ModuleTask]] =
    Seq(SourceActions, OutputActions)

  /**
   * Given a request context, lists the shown tabs.
   */
  override def tabs(context: Context[ModuleTask]): Seq[Tab] = Seq.empty

  object SourceActions extends TaskActions[SourceTask] {

    /** The name of the task type */
    override def name: String = "Source"

    /** Path to the task icon */
    override def icon: String = "workspace/img/server.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/dialogs/newSource/$project")

    /** The path to the dialog for editing an existing task. */
    override def editDialog(project: String, task: String) =
      Some(s"workspace/dialogs/editSource/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      None

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"/api/workspace/$project/source/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: ModuleTask): Seq[(String, String)] = {
      task.asInstanceOf[SourceTask].source.dataSource match {
        case DataSource(_, params) => params.toSeq
      }
    }
  }

  object OutputActions extends TaskActions[OutputTask] {

    /** The name of the task type */
    override def name: String = "Output"

    /** Path to the task icon */
    override def icon: String = "workspace/img/server--arrow.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/dialogs/newOutput/$project")

    /** The path to the dialog for editing an existing task. */
    override def editDialog(project: String, task: String) =
      Some(s"workspace/dialogs/editOutput/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      None

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"/api/workspace/$project/output/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: ModuleTask): Seq[(String, String)] = {
      task.asInstanceOf[OutputTask].output.writer match {
        case DataWriter(_, params) => params.toSeq
      }
    }
  }
}
