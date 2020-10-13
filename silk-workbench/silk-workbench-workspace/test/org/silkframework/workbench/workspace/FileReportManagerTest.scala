package org.silkframework.workbench.workspace

import java.io.File
import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityExecution, ActivityExecutionMetaData, ActivityExecutionResult}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.util.FileUtils._
import org.silkframework.workspace.reports.ReportManager
import play.api.libs.json.{JsObject, Json}

class FileReportManagerTest extends FlatSpec with Matchers {

  behavior of "FileReportManager"

  it should "store and retrieve reports" in {
    withReportManager { reportManager =>
      val report = loadReport("workflowReport.json")
      val executionResult = ActivityExecutionResult(metaData = ActivityExecutionMetaData(), resultValue = Some(loadReport("workflowReport.json")))
      reportManager.addReport("project", "task", executionResult)
      val reports = reportManager.listReports(Some("project"), Some("task"))
      reports should have size 1

      val retrievedReport = reportManager.retrieveReport("project", "task", reports.head.time).resultValue.get
      retrievedReport shouldEqual report
    }
  }

  private def withReportManager(f: ReportManager => Unit): Unit = {
    val tempDir = Files.createTempDirectory("Silk_FileReportManagerTest").toFile
    try {
      val reportManager = FileReportManager(tempDir.getPath)
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
