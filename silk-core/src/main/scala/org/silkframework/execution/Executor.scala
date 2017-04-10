package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.SchemaTrait
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}

/**
  * Executes a task specification.
  *
  * @tparam TaskType      The supported task type, e.g., TransformSpecification
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
trait Executor[TaskType <: TaskSpec, ExecType <: ExecutionType] {

  def execute(task: Task[TaskType], inputs: Seq[ExecType#DataType], outputSchema: Option[SchemaTrait],
              execution: ExecType, context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)): Option[ExecType#DataType]

}
