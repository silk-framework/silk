package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext

/** Executes the delete files operator. */
case class LocalDeleteFilesOperatorExecutor() extends LocalExecutor[DeleteFilesOperator] {

  override def execute(task: Task[DeleteFilesOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val executionReport = DeleteFilesOperatorExecutionReportUpdater(task, context)
    val regex = task.data.filesRegex.trim.r
    val resourceManager = pluginContext.resources
    val filesToDelete = resourceManager.listRecursive
      .filter(f => regex.findFirstIn(f).isDefined)
    for(file <- filesToDelete) {
      resourceManager.delete(file)
      executionReport.increaseEntityCounter()
    }
    executionReport.executionDone()
    None
  }
}

case class DeleteFilesOperatorExecutionReportUpdater(task: Task[TaskSpec],
                                                     context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def operationLabel: Option[String] = Some("delete files")

  override def entityLabelSingle: String = "file"

  override def entityLabelPlural: String = "files"

  override def entityProcessVerb: String = "deleted"

  override def minEntitiesBetweenUpdates: Int = 1
}