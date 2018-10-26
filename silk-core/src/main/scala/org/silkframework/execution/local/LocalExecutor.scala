package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{ExecutionReport, Executor}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}

/**
  * Executor that runs in a local context
  */
trait LocalExecutor[TaskType <: TaskSpec] extends Executor[TaskType, LocalExecution] {
  override def execute(task: Task[TaskType], inputs: Seq[LocalEntities], outputSchema: Option[EntitySchema],
                       execution: LocalExecution, context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext): Option[LocalEntities]
}
