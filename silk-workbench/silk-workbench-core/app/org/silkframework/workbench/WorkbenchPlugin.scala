package org.silkframework.workbench

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.ProjectTask
import play.api.mvc.Call

import scala.reflect.ClassTag

/**
  * Specifies the UI integration of specific task types.
  */
abstract class WorkbenchPlugin[T <: TaskSpec : ClassTag] {

  /**
    * The task types that are covered by this plugin.
    */
  def taskType: TaskType

  /**
    * The task actions that are provided for a specific task.
    */
  def taskActions(task: ProjectTask[_ <: TaskSpec]): TaskActions

  def taskClass: Class[_] = {
    implicitly[ClassTag[T]].runtimeClass
  }

  def isCompatible(task: ProjectTask[_ <: TaskSpec]): Boolean = {
    taskClass.isAssignableFrom(task.data.getClass)
  }
}

object WorkbenchPlugin {

  trait TaskType {

    def typeName: String

    def createDialog(project: String): Option[String]

    /** Path to the task icon */
    def icon: String

    /** Path to the task folder icon */
    def folderIcon: String

    /** Tasks with a higher indices are shown before tasks with lower indices. */
    def index: Int = Int.MaxValue

  }

  trait TaskActions {

    /** The name of the task type */
    def taskType: TaskType

    def task: ProjectTask[_ <: TaskSpec]

    /** The path to the dialog for editing an existing task. */
    def propertiesDialog: Option[String]

    /** The path to redirect to when the task is opened. */
    def openPath(inWorkflow: Option[Identifier] = None, workflowOperatorId: Option[String]): Option[String]

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

  object Tab {
    def apply(title: String, call: Call): Tab = new Tab(title, call.path())
  }

}