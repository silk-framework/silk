package org.silkframework.workspace.reports

import java.time.Instant

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier

@Plugin(
  id = "none",
  label = "None",
  description = "Discards execution reports and does not persist them."
)
case class EmptyExecutionReportManager() extends ExecutionReportManager {

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportMetaData] = Seq.empty

  override def retrieveReport(projectId: Identifier, taskId: Identifier, time: Instant): ActivityExecutionResult[ExecutionReport] = throw new NoSuchElementException

  override def addReport(projectId: Identifier, taskId: Identifier, report: ActivityExecutionResult[ExecutionReport]): Unit = { }
}