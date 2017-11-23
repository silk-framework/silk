import org.silkframework.config.Prefixes
import org.silkframework.workspace.activity.workflow.Workflow
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}

/**
 * The workflow Workbench plugin.
 */
case class WorkflowPlugin() extends WorkbenchPlugin {

  override def tasks = {
    Seq(WorkflowTaskActions)
  }

  override def tabs(context: Context[_]) = {
    var tabs = List[Tab]()
    if(context.task.data.isInstanceOf[Workflow]) {
      val p = context.project.name
      val t = context.task.id
      if (config.workbench.tabs.editor) {
        tabs ::= Tab("Editor", s"workflow/editor/$p/$t")
        tabs ::= Tab("Report", s"workflow/report/$p/$t")
      }
    }
    tabs.reverse
  }

  object WorkflowTaskActions extends TaskActions[Workflow] {

    /** The name of the task type */
    override def name: String = "Workflow"

    /** Path to the task icon */
    override def icon: String = controllers.workflow.routes.Assets.at("img/arrow-switch.png").url

    override def folderIcon: String = controllers.workflow.routes.Assets.at("img/workflow-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workflow/dialogs/$project/workflowDialog")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      None

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"workflow/editor/$project/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: Any)(implicit prefixes: Prefixes): Seq[(String, String)] = {
      Seq()
    }
  }

}