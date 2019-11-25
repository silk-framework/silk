package org.silkframework.workbench.workspace

import controllers.workspace.routes.Assets
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin
import org.silkframework.workbench.WorkbenchPlugin.{Tab, TaskActions, TaskType}
import org.silkframework.workbench.workspace.WorkbenchPluginCustomTask.{CustomTaskActions, CustomTaskType}
import org.silkframework.workspace.ProjectTask

import scala.language.existentials

case class WorkbenchPluginCustomTask() extends WorkbenchPlugin[CustomTask] {

  override def taskType: TaskType = CustomTaskType

  override def taskActions(task: ProjectTask[_ <: TaskSpec]): TaskActions = {
    CustomTaskActions(task)
  }
}

object WorkbenchPluginCustomTask {

  object CustomTaskType extends TaskType {

    /** The name of the task type */
    override def typeName: String = "Other"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/task.png").url

    override def folderIcon: String = Assets.at("img/task-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/customTasks/newTaskDialog/$project")
  }

  case class CustomTaskActions(task: ProjectTask[_ <: TaskSpec]) extends TaskActions {

    private val project = task.project.name

    private val taskId = task.id

    /** The name of the task type */
    override def taskType: TaskType = CustomTaskType

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog =
      Some(s"workspace/customTasks/editTaskDialog/$project/$taskId")

    /** The path to redirect to when the task is opened. */
    override def openPath(inWorkflow: Option[Identifier], workflowOperatorId: Option[String]): Option[String] =
      None

    /**
      * Lists the shown tabs.
      */
    override def tabs: Seq[Tab] = Seq.empty
  }
}
