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

  def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportMetaData]

  /**
    * Retrieves a report.
    *
    * @param projectId
    * @param taskId
    * @param time
    *
    * @throws NoSuchElementException If no report for the given project, task and time does exist.
    */
  def retrieveReport(projectId: Identifier, taskId: Identifier, time: Instant): ActivityExecutionResult[ExecutionReport]

  def addReport(projectId: Identifier, taskId: Identifier, report: ActivityExecutionResult[ExecutionReport]): Unit

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

  def apply(): ExecutionReportManager = instance

}
