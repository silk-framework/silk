package org.silkframework.workspace.reports

import java.time.Instant
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.plugin.{AnyPlugin, PluginRegistry}
import org.silkframework.util.Identifier

trait ReportManager extends AnyPlugin {

  def listReports(projectId: Identifier, taskId: Identifier): Seq[Instant]

  /**
    * Retrieves a report.
    *
    * @param projectId
    * @param taskId
    * @param time
    *
    * @throws NoSuchElementException If no report for the given project, task and time does exist.
    */
  def retrieveReport(projectId: Identifier, taskId: Identifier, time: Instant): ExecutionReport

  def addReport(projectId: Identifier, taskId: Identifier, report: ExecutionReport): Unit

}

case class EmptyReportManager() extends ReportManager {

  override def listReports(projectId: Identifier, taskId: Identifier): Seq[Instant] = Seq.empty

  override def retrieveReport(projectId: Identifier, taskId: Identifier, time: Instant): ExecutionReport = throw new NoSuchElementException

  override def addReport(projectId: Identifier, taskId: Identifier, report: ExecutionReport): Unit = { }
}

object ReportManager {

  private val log = Logger.getLogger(getClass.getName)

  private lazy val instance: ReportManager = {
    val config = DefaultConfig.instance()
    if (config.hasPath("workspace.reportManager")) {
      val repository = PluginRegistry.createFromConfig[ReportManager]("workspace.reportManager")
      log.info("Using configured report manager " + config.getString("workspace.reportManager.plugin"))
      repository
    } else {
      log.info("No report manager configured at configuration path 'workspace.reportManager.*'. No reports will be persistet.")
      EmptyReportManager()
    }
  }

  def apply(): ReportManager = instance

}
