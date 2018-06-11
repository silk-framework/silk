package org.silkframework.workbench.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin
import org.silkframework.workbench.WorkbenchPlugin.{Tab, TaskActions, TaskType}
import org.silkframework.workbench.workflow.WorkflowPlugin.{WorkflowTaskActions, WorkflowTaskType}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.Workflow

import scala.language.existentials

case class WorkflowPlugin() extends WorkbenchPlugin[Workflow] {

  /**
    * The task types that are covered by this plugin.
    */
  override def taskType: TaskType = WorkflowTaskType

  /**
    * The task actions that are provided for a specific task.
    */
  override def taskActions(task: ProjectTask[_ <: TaskSpec]): TaskActions = {
    WorkflowTaskActions(task)
  }

}

object WorkflowPlugin {

  object WorkflowTaskType extends TaskType {

    /** The name of the task type */
    override def typeName: String = "Workflow"

    /** Path to the task icon */
    override def icon: String = controllers.workflow.routes.Assets.at("img/arrow-switch.png").url

    override def folderIcon: String = controllers.workflow.routes.Assets.at("img/workflow-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workflow/dialogs/$project/workflowDialog")

    override def index: Int = 3
  }

  case class WorkflowTaskActions(task: ProjectTask[_ <: TaskSpec]) extends TaskActions {

    private val project = task.project.name

    private val taskId = task.id

    /** The name of the task type */
    override def taskType: TaskType = WorkflowTaskType

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog = None

    /** The path to redirect to when the task is opened. */
    override def openPath(inWorkflow: Option[Identifier]) =
      Some(s"workflow/editor/$project/$taskId")

    /**
      * Lists the shown tabs.
      */
    override def tabs = {
      var tabs = List[Tab]()
      if(task.data.isInstanceOf[Workflow]) {
        tabs ::= Tab("Editor", s"workflow/editor/$project/$taskId")
        tabs ::= Tab("Report", s"workflow/report/$project/$taskId")
      }
      tabs.reverse
    }
  }

}
