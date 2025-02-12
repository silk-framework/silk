package org.silkframework.execution

import org.silkframework.config.{FlexibleSchemaPort, Port, Task, TaskSpec, UnknownSchemaPort}
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

  def execute(task: Task[TaskType], inputs: Seq[ExecType#DataType], output: ExecutorOutput,
              execution: ExecType, context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
             (implicit pluginContext: PluginContext): Option[ExecType#DataType]

}

/**
  * The output parameters of an execution.
  * @param task                  Optional output task. Not defined if no output task to write to exists or
  *                              the output task is not available for other reasons.
  * @param connectedPort         The connected output port.
  */
case class ExecutorOutput(task: Option[Task[_ <: TaskSpec]], connectedPort: Port) {

  /**
   * The requested output, which should only be used by tasks that allow to react to such requests, e.g. datasets.
   */
  def requestedSchema: Option[EntitySchema] = connectedPort.schemaOpt

  def compatibleWithDataset: Boolean = connectedPort.schemaOpt.isDefined || connectedPort == FlexibleSchemaPort
}

object ExecutorOutput {
  final val empty = ExecutorOutput(None, UnknownSchemaPort)
}