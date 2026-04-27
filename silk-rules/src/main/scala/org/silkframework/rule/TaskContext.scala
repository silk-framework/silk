package org.silkframework.rule

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.plugin.PluginContext

/**
 * The context in which a task is executed.
 *
 * @param inputTasks The input tasks.
 *                   If the task is executed within a workflow, those are the connected input task(s).
 *                   If the task is executed standalone, those are the configured default input(s).
 */
case class TaskContext(inputTasks: Seq[Task[_ <: TaskSpec]], pluginContext: PluginContext)

object TaskContext {

  /**
   * Creates a task context for a single input task.
   */
  def forInput(inputTask: Task[_ <: TaskSpec])(implicit pluginContext: PluginContext): TaskContext = {
    TaskContext(Seq(inputTask), pluginContext)
  }

  /**
   * Creates a task context for the given input tasks.
   */
  def forInputs(inputTasks: Seq[Task[_ <: TaskSpec]])(implicit pluginContext: PluginContext): TaskContext = {
    TaskContext(inputTasks, pluginContext)
  }

  /**
   * Creates a task context for no input.
   */
  def noInput(implicit pluginContext: PluginContext): TaskContext = {
    TaskContext(Seq.empty, pluginContext)
  }

}