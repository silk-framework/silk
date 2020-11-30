package org.silkframework.workbench.workspace

import java.nio.file.Files
import java.time.Duration

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityExecutionMetaData, ActivityExecutionResult}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.util.FileUtils._
import org.silkframework.workbench.workspace.FileExecutionReportManager.DEFAULT_RETENTION_TIME
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.Json

class FileExecutionReportManagerTest extends FlatSpec with Matchers {

  behavior of "FileReportManager"

  private val testReport = loadReport("workflowReport.json")
  private val testReportResult = ActivityExecutionResult(ActivityExecutionMetaData(), Some(testReport))

  it should "store and retrieve reports" in {
    withReportManager() { reportManager =>
      reportManager.addReport(ReportIdentifier.create("project", "task"), testReportResult)

      // Make sure that the report will only be retrieved if project and task match
      reportManager.listReports(Some("project1"), Some("task")) should have size 0
      reportManager.listReports(Some("project"), Some("task1")) should have size 0

      // Make sure that retrieved report is equal to the committed one
      val reports = reportManager.listReports(Some("project"), Some("task"))
      reports should have size 1
      val retrievedReport = reportManager.retrieveReport(reports.head).resultValue.get
      retrievedReport.toString shouldEqual testReport.toString
    }
  }

  it should "delete reports after the retention time has been reached" in {
    val retentionTimeInMillis = 500
    withReportManager(Duration.ofMillis(retentionTimeInMillis)) { reportManager =>
      reportManager.addReport(ReportIdentifier.create("project", "task"), testReportResult)
      reportManager.addReport(ReportIdentifier.create("project", "task"), testReportResult)
      reportManager.listReports(Some("project"), Some("task")) should have size 2

      Thread.sleep(retentionTimeInMillis)

      // Adding a third report should delete both of the previous ones because their retention time is exceeded
      reportManager.addReport(ReportIdentifier.create("project", "task"), testReportResult)
      reportManager.listReports(Some("project"), Some("task")) should have size 1
    }
  }

  private def withReportManager(retentionTime: Duration = DEFAULT_RETENTION_TIME)(f: ExecutionReportManager => Unit): Unit = {
    val tempDir = Files.createTempDirectory("Silk_FileReportManagerTest").toFile
    try {
      val reportManager = FileExecutionReportManager(tempDir.getPath, retentionTime)
      f(reportManager)
    } finally {
      tempDir.deleteRecursive()
    }
  }

  private def loadReport(name: String): ExecutionReport = {
    val inputStream = getClass.getResourceAsStream(name)
    try {
      implicit val rc: ReadContext = ReadContext()
      ExecutionReportSerializers.ExecutionReportJsonFormat.read(Json.parse(inputStream))
    } finally {
      inputStream.close()
    }
  }

}
