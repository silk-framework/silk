package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema, FileType}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext

import scala.collection.mutable

/** Executes the get project files operator. */
case class LocalGetProjectFilesOperatorExecutor() extends LocalExecutor[GetProjectFilesOperator] {

  override def execute(task: Task[GetProjectFilesOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val executionReport = GetProjectFilesOperatorExecutionReportUpdater(task, context)

    val fileEntities =
      for(filePath <- task.data.getFiles()) yield {
        val file = pluginContext.resources.getInPath(filePath)
        executionReport.addFile(filePath)
        FileEntity(file, FileType.Project)
      }

    executionReport.executionDone()
    Some(FileEntitySchema.create(fileEntities, task))
  }
}

case class GetProjectFilesOperatorExecutionReportUpdater(task: Task[TaskSpec],
                                                         context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
  private val files = mutable.Buffer[String]()

  override def operationLabel: Option[String] = Some("retrieve files")

  override def entityLabelSingle: String = "file"

  override def entityLabelPlural: String = "files"

  override def entityProcessVerb: String = "retrieved"

  override def minEntitiesBetweenUpdates: Int = 1

  def addFile(file: String): Unit = {
    files.append(file)
    increaseEntityCounter()
  }

  override def additionalFields(): Seq[(String, String)] = {
    Seq(
      "Number of retrieved files" -> (if(files != null) files.size.toString else "0"),
      "Retrieved files" -> (if(files != null) files.mkString(", ") else ""),
    )
  }
}