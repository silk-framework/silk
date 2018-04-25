package org.silkframework.workbench

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workbench.WorkbenchPlugin.{TaskActions, TaskType}
import org.silkframework.workspace.{Project, ProjectTask}

object WorkbenchPlugins {

  private lazy val allPlugins: Seq[WorkbenchPlugin] = {
    PluginRegistry.availablePlugins[WorkbenchPlugin].map(_.apply()(Prefixes.empty))
  }

  def byType(project: Project): Seq[(TaskType, Seq[TaskActions])] = {
    val plugins = for(task <- project.allTasks; plugin <- allPlugins; taskActions <- plugin.taskActions(task)) yield taskActions
    plugins.groupBy(_.taskType).toSeq.sortBy(_._1.typeName)
  }

  def forTask(task: ProjectTask[_ <: TaskSpec]): Option[TaskActions] = {
    allPlugins.flatMap(_.taskActions(task)).headOption
  }

}
