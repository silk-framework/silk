package org.silkframework.workbench.workspace

import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityExecutionMetaData, ActivityExecutionResult}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.util.FileUtils._
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.Json

class FileExecutionReportManagerTest extends FlatSpec with Matchers {

  behavior of "FileReportManager"

  it should "store and retrieve reports" in {
    withReportManager { reportManager =>
      val report = loadReport("workflowReport.json")
      val executionResult = ActivityExecutionResult(metaData = ActivityExecutionMetaData(), resultValue = Some(loadReport("workflowReport.json")))
      reportManager.addReport(ReportIdentifier.create("project", "task"), executionResult)
      val reports = reportManager.listReports(Some("project"), Some("task"))
      reports should have size 1

      val retrievedReport = reportManager.retrieveReport(reports.head).resultValue.get
      retrievedReport.toString shouldEqual report.toString
    }
  }

  private def withReportManager(f: ExecutionReportManager => Unit): Unit = {
    val tempDir = Files.createTempDirectory("Silk_FileReportManagerTest").toFile
    try {
      val reportManager = FileExecutionReportManager(tempDir.getPath)
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
