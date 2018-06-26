package org.silkframework.workbench

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.{Project, ProjectTask}

object WorkbenchPlugins {

  /**
    * Holds all registered workbench plugins.
    */
  private lazy val allPlugins: Seq[WorkbenchPlugin[_ <: TaskSpec]] = {
    PluginRegistry.availablePlugins[WorkbenchPlugin[_ <: TaskSpec]].map(_.apply()(Prefixes.empty)).sortBy(_.taskType.typeName)
  }

  /**
    * Given a project, returns all tasks actions grouped by task type.
    */
  def byType(project: Project): Seq[(TaskType, Seq[TaskActions])] = {
    for {
      plugin <- allPlugins
    } yield {
      (
        plugin.taskType,
        for {
          task <- project.allTasks
          taskPlugin = pluginForTask(task) if taskPlugin.getClass == plugin.getClass
        } yield taskPlugin.taskActions(task)
      )
    }
  }.sortBy(_._1.index)

  /**
    * Given a task, returns the available task actions.
    */
  def forTask(task: ProjectTask[_ <: TaskSpec]): TaskActions = {
    pluginForTask(task).taskActions(task)
  }

  private def pluginForTask(task: ProjectTask[_ <: TaskSpec]): WorkbenchPlugin[_ <: TaskSpec] = {
    allPlugins.filter(_.isCompatible(task)) match {
      case Seq() =>
        throw new ValidationException(s"No WorkbenchPlugin registered for task type ${task.data.getClass.getName}.")
      case Seq(plugin) =>
        plugin
      case plugins =>
        // There are multiple plugins that cover this task type => Choose the most specific one
        plugins.sortWith { case (t1, t2) => t2.taskClass.isAssignableFrom(t1.taskClass) }.head
    }
  }

}
