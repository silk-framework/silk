package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.operations.ClearDatasetOperator.ClearDatasetTable
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext

/** Executes a clear dataset operator. */
case class ClearDatasetOperatorLocalExecutor() extends LocalExecutor[ClearDatasetOperator] {

  override def execute(task: Task[ClearDatasetOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    Some(ClearDatasetTable(task))
  }
}

case class ClearDatasetOperatorExecutionReportUpdater(task: Task[TaskSpec],
                                                      context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def operationLabel: Option[String] = Some("clear dataset")

  override def minEntitiesBetweenUpdates: Int = 1

  override def additionalFields(): Seq[(String, String)] = Seq(
    "Cleared dataset" -> task.fullLabel
  )
}