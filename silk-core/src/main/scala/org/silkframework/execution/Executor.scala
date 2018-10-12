package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}

/**
  * Executes a task specification.
  *
  * @tparam TaskType      The supported task type, e.g., TransformSpecification
  * @tparam ExecType The execution type, e.g., SparkExecution
  */
trait Executor[TaskType <: TaskSpec, ExecType <: ExecutionType] {

  def execute(task: Task[TaskType], inputs: Seq[ExecType#DataType], outputSchema: Option[EntitySchema],
              execution: ExecType, context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
             (implicit userContext: UserContext): Option[ExecType#DataType]

}
