import de.fuberlin.wiwiss.silk.dataset.rdf.RdfDatasetPlugin
import de.fuberlin.wiwiss.silk.dataset.{ DatasetPlugin => DataPlugin}
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}

/**
 * The data plugin adds data sources and outputs.
 */
case class DatasetPlugin() extends WorkbenchPlugin {
  /**
   * The task types to be added to the Workspace.
   */
  override def tasks: Seq[TaskActions[ModuleTask]] =
    Seq(DatasetActions)

  /**
   * Given a request context, lists the shown tabs.
   */
  override def tabs(context: Context[ModuleTask]): Seq[Tab] = {
    val p = context.project.name
    val t = context.task.name
    var tabs = Seq(Tab("Dataset", s"workspace/datasets/$p/$t/dataset"))
    context.task match {
      case task: DatasetTask =>
        if (task.dataset.plugin.isInstanceOf[RdfDatasetPlugin] ) {
          tabs = tabs :+ Tab ("Sparql", s"workspace/datasets/$p/$t/sparql")
        }
      case _ =>
    }
    tabs
  }

  object DatasetActions extends TaskActions[DatasetTask] {

    /** The name of the task type */
    override def name: String = "Dataset"

    /** Path to the task icon */
    override def icon: String = "img/server.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/dialogs/newDataset/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"workspace/dialogs/editDataset/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"workspace/datasets/$project/$task/dataset")

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"workspace/projects/$project/dataset/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: ModuleTask): Seq[(String, String)] = {
      task.asInstanceOf[DatasetTask].dataset.plugin match {
        case DataPlugin(_, params) => params.toSeq
      }
    }
  }
}
