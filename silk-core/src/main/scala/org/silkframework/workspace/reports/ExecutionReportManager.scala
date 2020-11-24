package org.silkframework.workspace.reports

import java.time.Instant
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{AnyPlugin, PluginRegistry}
import org.silkframework.util.Identifier

trait ExecutionReportManager extends AnyPlugin {

  /**
    * Lists all available reports.
    *
    * @param projectId If provided, only reports for the given project are returned.
    * @param taskId If provided, only reports for the given task are returned.
    */
  def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportIdentifier]

  /**
    * Retrieves a report.
    *
    * @throws NoSuchElementException If no report for the given project, task and time does exist.
    */
  def retrieveReport(reportId: ReportIdentifier): ActivityExecutionResult[ExecutionReport]

  /**
    * Adds a new report.
    */
  def addReport(reportId: ReportIdentifier, report: ActivityExecutionResult[ExecutionReport]): Unit

  /**
    * Removes a report.
    */
  def removeReport(reportId: ReportIdentifier): Unit

}

object ExecutionReportManager {

  private val log = Logger.getLogger(getClass.getName)

  private lazy val instance: ExecutionReportManager = {
    val config = DefaultConfig.instance()
    if (config.hasPath("workspace.reportManager")) {
      val repository = PluginRegistry.createFromConfig[ExecutionReportManager]("workspace.reportManager")
      log.info("Using configured report manager " + config.getString("workspace.reportManager.plugin"))
      repository
    } else {
      log.info("No report manager configured at configuration path 'workspace.reportManager.*'. No reports will be persisted.")
      EmptyExecutionReportManager()
    }
  }

  /**
    * Retrieves the configured execution report manager.
    */
  def apply(): ExecutionReportManager = instance

}
