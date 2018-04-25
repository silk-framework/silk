package org.silkframework.workbench

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.ProjectTask

/**
  * Specifies the UI integration of specific task types.
  */
trait WorkbenchPlugin {

  /**
    * The task types that are covered by this plugin.
    */
  def taskTypes: Seq[TaskType]

  /**
    * The task actions that are provided for a specific task.
    */
  def taskActions(task: ProjectTask[_ <: TaskSpec]): Option[TaskActions]

}

object WorkbenchPlugin {

  trait TaskType {

    def typeName: String

    def createDialog(project: String): Option[String]

    /** Path to the task icon */
    def icon: String

    /** Path to the task folder icon */
    def folderIcon: String

  }

  trait TaskActions {

    /** The name of the task type */
    def taskType: TaskType

    def task: ProjectTask[_ <: TaskSpec]

    /** The path to the dialog for editing an existing task. */
    def propertiesDialog: Option[String]

    /** The path to redirect to when the task is opened. */
    def openPath: Option[String]

    /** The paths to the tabs that should be shown for this task. */
    def tabs: Seq[Tab]
  }

  /**
    * A tab in the tabbar.
    *
    * @param title The title that is displayed on the tab.
    * @param path The target when the user clicks on the tab.
    */
  case class Tab(title: String, path: String)

}