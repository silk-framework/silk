package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.{ExecutionReport, Executor, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext

/**
  * Executor that runs in a local context
  */
trait LocalExecutor[TaskType <: TaskSpec] extends Executor[TaskType, LocalExecution] {
  override def execute(task: Task[TaskType], inputs: Seq[LocalEntities], output: ExecutorOutput,
                       execution: LocalExecution, context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities]
}
