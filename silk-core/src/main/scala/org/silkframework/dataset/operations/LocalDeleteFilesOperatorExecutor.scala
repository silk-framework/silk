package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.report.EntitySample
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.iterator.CloseableIterator
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
    val filesToDelete = task.data.getFilesToDelete()
    for(file <- filesToDelete) {
      pluginContext.resources.delete(file)
      executionReport.addFile(file)
      executionReport.addSampleEntity(EntitySample(file))
      executionReport.increaseEntityCounter()
    }
    executionReport.executionDone()
    if(task.data.outputEntities) {
      executionReport.startNewOutputSamples(DeleteFilesOperator.schema)(pluginContext.prefixes)
      Some(GenericEntityTable(
        CloseableIterator(filesToDelete.map(filePath => Entity("deletedFile", IndexedSeq(Seq(filePath)), DeleteFilesOperator.schema))),
        DeleteFilesOperator.schema,
        task
      ))
    } else {
      None
    }
  }
}

case class DeleteFilesOperatorExecutionReportUpdater(task: Task[TaskSpec],
                                                     context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
  private var files = Vector.empty[String]

  override def operationLabel: Option[String] = Some("delete files")

  override def entityLabelSingle: String = "file"

  override def entityLabelPlural: String = "files"

  override def entityProcessVerb: String = "deleted"

  override def minEntitiesBetweenUpdates: Int = 1

  def addFile(file: String): Unit = {
    files = files appended file
  }

  override def additionalFields(): Seq[(String, String)] = {
    // files is still null when this method is called in the constructor of ExecutionReportUpdater
    val deletedFiles = if(files != null) files.mkString(", ") else ""
    Seq(
      "Deleted files" -> deletedFiles
    )
  }
}