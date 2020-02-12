package org.silkframework.workbench

import org.silkframework.config.{CustomTask, Prefixes, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.{Project, ProjectTask}

object WorkbenchPlugins {

  /**
    * Holds all registered workbench plugins.
    */
  private lazy val allPlugins: Seq[WorkbenchPlugin[_ <: TaskSpec]] = {
    val plugins = PluginRegistry.availablePlugins[WorkbenchPlugin[_ <: TaskSpec]].map(_.apply()(Prefixes.empty)).sortBy(_.taskType.typeName)

    // If a WorkbenchPlugin covers a custom task, we only load it, if that custom task is actually loaded as a plugin (and not blacklisted)
    val customTaskTypes = PluginRegistry.availablePlugins[CustomTask].map(_.pluginClass)
    plugins.filter(p => !classOf[CustomTask].isAssignableFrom(p.taskClass) || customTaskTypes.exists(p.taskClass.isAssignableFrom) )
  }

  /**
    * Given a project, returns all tasks actions grouped by task type.
    */
  def byType(project: Project)
            (implicit userContext: UserContext): Seq[(TaskType, Seq[TaskActions])] = {
    val allTasks =  project.allTasks
    for {
      plugin <- allPlugins
    } yield {
      (
        plugin.taskType,
        for {
          task <- allTasks
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
