package org.silkframework.workbench.workspace

import java.time.Duration
import org.silkframework.config.Prefixes
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityExecutionMetaData, ActivityExecutionResult}
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Base class for ExecutionReportManager implementation tests.
  */
abstract class ExecutionReportManagerTest extends AnyFlatSpec with Matchers {

  private implicit val pluginContext: PluginContext = TestPluginContext(resources = InMemoryResourceManager())

  private val projectId = "4e371d98-3de7-4986-ab7d-979612f1ac29_project"
  private val taskId = "4150f4a9-4104-4681-90f5-9fc64d4ecce0_workflow"
  private val testReport = loadReport("workflowReport.json")
  private val testReportResult = ActivityExecutionResult(ActivityExecutionMetaData(), Some(testReport))

  protected def withReportManager(retentionTime: Duration = ExecutionReportManager.DEFAULT_RETENTION_TIME)(f: ExecutionReportManager => Unit): Unit

  it should "store and retrieve reports" in {
    withReportManager() { reportManager =>
      reportManager.addReport(ReportIdentifier.create(projectId, taskId), testReportResult)

      // Make sure that the report will only be retrieved if project and task match
      reportManager.listReports(Some(projectId + "x"), Some(taskId)) should have size 0
      reportManager.listReports(Some(projectId), Some(taskId + "x")) should have size 0

      // Make sure that retrieved report is equal to the committed one
      val reports = reportManager.listReports(Some(projectId), Some(taskId))
      reports should have size 1
      val retrievedReport = reportManager.retrieveReport(reports.head).resultValue.get
      retrievedReport.toString.replaceAll("Vector", "List") shouldEqual testReport.toString.replaceAll("Vector", "List")
    }
  }

  it should "delete reports after the retention time has been reached" in {
    val retentionTimeInMillis = 500
    withReportManager(Duration.ofMillis(retentionTimeInMillis)) { reportManager =>
      reportManager.addReport(ReportIdentifier.create(projectId, taskId), testReportResult)
      Thread.sleep(1)
      reportManager.addReport(ReportIdentifier.create(projectId, taskId), testReportResult)
      reportManager.listReports(Some(projectId), Some(taskId)) should have size 2

      Thread.sleep(retentionTimeInMillis)

      // Adding a third report should delete both of the previous ones because their retention time is exceeded
      reportManager.addReport(ReportIdentifier.create(projectId, taskId), testReportResult)
      reportManager.listReports(Some(projectId), Some(taskId)) should have size 1
    }
  }

  private def loadReport(name: String): ExecutionReport = {
    val inputStream = getClass.getResourceAsStream(name)
    try {
      implicit val rc: ReadContext = ReadContext.fromPluginContext()(pluginContext)
      ExecutionReportSerializers.ExecutionReportJsonFormat.read(Json.parse(inputStream))
    } finally {
      inputStream.close()
    }
  }

}
