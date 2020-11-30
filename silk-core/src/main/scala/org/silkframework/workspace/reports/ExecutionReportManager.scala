package org.silkframework.workspace.reports

import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import org.silkframework.config.DefaultConfig
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.{AnyPlugin, PluginRegistry}
import org.silkframework.util.Identifier

import scala.util.control.NonFatal

trait ExecutionReportManager extends AnyPlugin {

  protected val log: Logger = Logger.getLogger(getClass.getName)

  /**
    * Lists all available reports.
    *
    * @param projectId If provided, only reports for the given project are returned.
    * @param taskId If provided, only reports for the given task are returned.
    */
  def listReports(projectId: Option[Identifier] = None, taskId: Option[Identifier] = None): Seq[ReportIdentifier]

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

  /**
    * Removes reports older than the retention time.
    * If removal of a report fails, a warning will be logged and the method will return normally.
    */
  protected def removeOldReports(retentionTime: Duration): Unit = {
    val oldestDateTime = Instant.now.minus(retentionTime)
    for (reportId <- listReports(None, None) if reportId.time.isBefore(oldestDateTime)) {
      try {
        removeReport(reportId)
      } catch {
        case NonFatal(ex) =>
          log.log(Level.WARNING, s"Could not delete report " + reportId, ex)
      }
    }
  }

}

object ExecutionReportManager {

  private val log = Logger.getLogger(getClass.getName)

  val DEFAULT_RETENTION_TIME: Duration = Duration.ofDays(30)

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
