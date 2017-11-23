import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.dataset.{Dataset, DatasetTask}
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}
import controllers.workspace.routes.Assets

/**
 * The data plugin adds data sources and outputs.
 */
case class WorkbenchDatasetPlugin() extends WorkbenchPlugin {
  /**
   * The task types to be added to the Workspace.
   */
  override def tasks: Seq[TaskActions[_ <: TaskSpec]] =
    Seq(DatasetActions)

  /**
   * Given a request context, lists the shown tabs.
   */
  override def tabs(context: Context[_]): Seq[Tab] = {
    val p = context.project.name
    val t = context.task.id
    context.task.data match {
      case dataset: DatasetTask =>
        var tabs = Seq(Tab("Dataset", s"workspace/datasets/$p/$t/dataset"))
        if (dataset.plugin.isInstanceOf[RdfDataset] ) {
          tabs = tabs :+ Tab("Sparql", s"workspace/datasets/$p/$t/sparql")
        } else {
          tabs = tabs :+ Tab("Tableview", s"workspace/datasets/$p/$t/table")
        }
        tabs
      case _ => Seq.empty
    }
  }

  object DatasetActions extends TaskActions[Dataset] {

    /** The name of the task type */
    override def name: String = "Dataset"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/server.png").url

    override def folderIcon: String = Assets.at("img/dataset-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/dialogs/newDataset/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"workspace/dialogs/editDataset/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"workspace/datasets/$project/$task/dataset")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(taskData: Any)(implicit prefixes: Prefixes): Seq[(String, String)] = {
      taskData.asInstanceOf[Dataset] match {
        case Dataset(_, params) => params.toSeq
      }
    }
  }
}
