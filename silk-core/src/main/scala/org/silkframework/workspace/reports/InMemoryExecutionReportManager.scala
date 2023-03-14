package org.silkframework.workspace.reports

import java.time.Duration
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier

import scala.collection.mutable

@Plugin(
  id = "inMemory",
  label = "Reports held in memory",
  description = "Holds the reports in memory."
)
case class InMemoryExecutionReportManager(retentionTime: Duration = ExecutionReportManager.DEFAULT_RETENTION_TIME) extends ExecutionReportManager {

  private val reports = mutable.ListMap[ReportIdentifier, ActivityExecutionResult[ExecutionReport]]()

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportIdentifier] = synchronized {
    for {
      reportId <- reports.keys.toSeq
      if projectId.forall(_ == reportId.projectId)
      if taskId.forall(_ == reportId.taskId)
    } yield {
      reportId
    }
  }

  override def retrieveReport(reportId: ReportIdentifier): ActivityExecutionResult[ExecutionReport] = synchronized {
    reports(reportId)
  }

  override def addReport(reportId: ReportIdentifier, report: ActivityExecutionResult[ExecutionReport])
                        (implicit pluginContext: PluginContext): Unit = synchronized {
    removeOldReports(retentionTime)
    reports += ((reportId, report))
  }

  override def removeReport(reportId: ReportIdentifier): Unit = synchronized {
    reports -= reportId
  }

}


