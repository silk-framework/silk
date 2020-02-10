package org.silkframework.execution

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}

/**
  * Executes a task specification.
  *
  * @tparam TaskType      The supported task type, e.g., TransformSpecification
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
trait Executor[TaskType <: TaskSpec, ExecType <: ExecutionType] {

  def execute(task: Task[TaskType], inputs: Seq[ExecType#DataType], output: ExecutorOutput,
              execution: ExecType, context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
             (implicit userContext: UserContext, prefixes: Prefixes): Option[ExecType#DataType]

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