package org.silkframework.workspace

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.{AnyPlugin, ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.util.Identifier

/** Cleans up task related data, e.g. after a task is deleted. Each registered implementation is called after a task gets deleted.
  * Implementations must not take any parameters. */
trait TaskCleanupPlugin extends AnyPlugin {
  def cleanUpAfterDeletion(projectId: Identifier, taskId: Identifier, taskSpec: TaskSpec): Unit
}

object TaskCleanupPlugin {
  // (projectId, taskId, task)
  type CleanUpAfterTaskDeletionFunction = (Identifier, Identifier, TaskSpec) => Unit

  implicit private val pluginContext: PluginContext = PluginContext.empty
  /** Returns a function that executes all task clean up plugins. */
  def retrieveCleanUpAfterTaskDeletionFunction: CleanUpAfterTaskDeletionFunction = {
    val cleanupPlugins = PluginRegistry.availablePlugins[TaskCleanupPlugin].map(pd =>
      pd(ParameterValues.empty)
    )
    (projectId: Identifier, taskId: Identifier, taskSpec: TaskSpec) => {
      cleanupPlugins.foreach(p => p.cleanUpAfterDeletion(projectId, taskId, taskSpec))
    }
  }
}