package org.silkframework.workspace

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}

/** Cleans up task related data, e.g. after a task is deleted. Each registered implementation is called after a task gets deleted.
  * Implementations must not take any parameters. */
trait TaskCleanupPlugin {
  def cleanUpAfterDeletion(projectId: String, taskId: String, taskSpec: TaskSpec): Unit
}

object TaskCleanupPlugin {
  implicit private val pluginContext: PluginContext = PluginContext.empty
  /** Returns a function that executes all task clean up plugins. */
  def cleanUpAfterTaskDeletionFunction: (String, String, TaskSpec) => Unit = {
    val cleanupPlugins = PluginRegistry.availablePlugins[TaskCleanupPlugin].map(pd =>
      pd(Map.empty)
    )
    (projectId: String, taskId: String, taskSpec: TaskSpec) => {
      cleanupPlugins.foreach(p => p.cleanUpAfterDeletion(projectId, taskId, taskSpec))
    }
  }
}