package org.silkframework.workbench.workspace

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.util.Identifier
import org.silkframework.workspace.reports.ReportManager
import play.api.libs.json.{JsValue, Json}

@Plugin(
  id = "file",
  label = "Reports on filesystem",
  description = "Holds the reports in a specified directory on the filesystem."
)
case class FileReportManager(dir: String) extends ReportManager {

  private val reportDirectory = new File(dir)
  reportDirectory.mkdirs()

  def retrieveReports(projectId: Identifier, taskId: Identifier): Seq[ExecutionReport] = {
    for(reportFile <- reportDirectory.listFiles().filter(_.getName.endsWith(".json"))) yield {
      val inputStream = new FileInputStream(reportFile)
      try {
        implicit val rc: ReadContext = ReadContext()
        ExecutionReportSerializers.ExecutionReportJsonFormat.read(Json.parse(inputStream))
      } finally {
        inputStream.close()
      }
    }
  }

  def addReport(projectId: Identifier, taskId: Identifier, report: ExecutionReport): Unit = {
    implicit val wc = WriteContext[JsValue]()
    val reportJson = ExecutionReportSerializers.ExecutionReportJsonFormat.write(report)

    Files.write(new File(reportDirectory, UUID.randomUUID.toString + ".json").toPath, Json.prettyPrint(reportJson).getBytes(StandardCharsets.UTF_8))
  }

}
