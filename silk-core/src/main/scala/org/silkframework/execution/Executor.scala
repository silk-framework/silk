package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext}

/**
  * Executes a task specification.
  *
  * @tparam TaskType      The supported task type, e.g., TransformSpecification
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
@PluginType()
trait Executor[TaskType <: TaskSpec, ExecType <: ExecutionType] extends AnyPlugin {

  def execute(task: Task[TaskType], inputs: Seq[EntityType[ExecType]], output: ExecutorOutput,
              execution: ExecType, context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
             (implicit pluginContext: PluginContext): Option[EntityType[ExecType]]

}

/**
  * The output parameters of an execution.
  * @param task                  Optional output task. It not defined if no output task to write to exists or
  *                              the output task is not available for other reasons.
  * @param requestedSchema The requested output, which should only be used by tasks that allow to react to such
  *                              requests, e.g. datasets.
  */
case class ExecutorOutput(task: Option[Task[_ <: TaskSpec]], requestedSchema: Option[EntitySchema])

object ExecutorOutput {
  final val empty = ExecutorOutput(None, None)
}