package org.silkframework.workbench.rules

import controllers.rules.routes.Assets
import org.silkframework.config.TaskSpec
import org.silkframework.rule.LinkSpec
import org.silkframework.workbench.WorkbenchPlugin
import org.silkframework.workbench.WorkbenchPlugin.{Tab, TaskActions, TaskType}
import org.silkframework.workbench.rules.LinkingPlugin.{LinkingTaskActions, LinkingTaskType}
import org.silkframework.workspace.ProjectTask
import scala.language.existentials

/**
 * The linking Workbench plugin.
 */
case class LinkingPlugin() extends WorkbenchPlugin {

  /**
    * The task type that is covered by this plugin.
    */
  override def taskType: TaskType = LinkingTaskType

  /**
    * The task actions that are provided for a specific task.
    */
  override def taskActions(task: ProjectTask[_ <: TaskSpec]): Option[TaskActions] = {
    if(classOf[LinkSpec].isAssignableFrom(task.data.getClass)) {
      Some(LinkingTaskActions(task))
    } else {
      None
    }
  }

}

object LinkingPlugin {

  object LinkingTaskType extends TaskType {

    /** The name of the task type */
    override def typeName: String = "Linking Task"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/arrow-join.png").url

    override def folderIcon: String = Assets.at("img/linking-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"linking/dialogs/newLinkingTask/$project")
  }

  case class LinkingTaskActions(task: ProjectTask[_ <: TaskSpec]) extends TaskActions {

    private val project = task.project.name

    private val taskId = task.id

    /** The name of the task type */
    override def taskType: TaskType = LinkingTaskType

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog =
      Some(s"linking/dialogs/editLinkingTask/$project/$taskId")

    /** The path to redirect to when the task is opened. */
    override def openPath =
      Some(s"linking/$project/$taskId/editor")

    /**
      * Lists the shown tabs.
      */
    override def tabs = {
      var tabs = List[Tab]()
      if(task.data.isInstanceOf[LinkSpec]) {
        if (config.workbench.tabs.editor)
          tabs ::= Tab("Editor", s"linking/$project/$taskId/editor")
        if (config.workbench.tabs.generateLinks)
          tabs ::= Tab("Generate Links", s"linking/$project/$taskId/generateLinks")
        if (config.workbench.tabs.learn)
          tabs ::= Tab("Learn", s"linking/$project/$taskId/learnStart")
        if (config.workbench.tabs.referenceLinks)
          tabs ::= Tab("Reference Links", s"linking/$project/$taskId/referenceLinks")
      }
      tabs.reverse
    }
  }

}
