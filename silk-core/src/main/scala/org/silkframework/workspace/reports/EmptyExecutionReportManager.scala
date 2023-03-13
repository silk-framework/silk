package org.silkframework.workspace.reports

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier

@Plugin(
  id = "none",
  label = "None",
  description = "Discards execution reports and does not persist them."
)
case class EmptyExecutionReportManager() extends ExecutionReportManager {

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportIdentifier] = Seq.empty

  override def retrieveReport(reportId: ReportIdentifier): ActivityExecutionResult[ExecutionReport] = throw new NoSuchElementException

  override def addReport(reportId: ReportIdentifier, report: ActivityExecutionResult[ExecutionReport])
                        (implicit pluginContext: PluginContext): Unit = { }

  override def removeReport(reportId: ReportIdentifier): Unit = { }
}