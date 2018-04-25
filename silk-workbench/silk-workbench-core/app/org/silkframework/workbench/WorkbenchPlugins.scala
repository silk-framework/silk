package org.silkframework.workbench

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.{Project, ProjectTask}

object WorkbenchPlugins {

  private lazy val allPlugins: Seq[WorkbenchPlugin] = {
    PluginRegistry.availablePlugins[WorkbenchPlugin].map(_.apply()(Prefixes.empty)).sortBy(_.taskType.typeName)
  }

  def byType(project: Project): Seq[(TaskType, Seq[TaskActions])] = {
    for {
      plugin <- allPlugins
    } yield {
      (plugin.taskType, project.allTasks.flatMap(plugin.taskActions))
    }
  }

  def forTask(task: ProjectTask[_ <: TaskSpec]): Option[TaskActions] = {
    allPlugins.flatMap(_.taskActions(task)).headOption
  }

}
