package org.silkframework.workbench.workspace

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ExecutionReportSerializers
import org.silkframework.util.Identifier
import org.silkframework.workspace.reports.{ReportManager, ReportMetaData}
import play.api.libs.json.{JsValue, Json}

import scala.util.Try

@Plugin(
  id = "file",
  label = "Reports on filesystem",
  description = "Holds the reports in a specified directory on the filesystem."
)
case class FileReportManager(dir: String) extends ReportManager {

  private val reportDirectory = new File(dir)
  reportDirectory.mkdirs()

  private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(ZoneOffset.UTC)

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportMetaData] = {
    for {
      reportFile <- reportDirectory.listFiles()
      report <- fromReportFile(reportFile)
      if projectId.forall(_ == report.projectId)
      if taskId.forall(_ == report.taskId)
    } yield {
     report
    }
  }

  override def retrieveReport(projectId: Identifier, taskId: Identifier, time: Instant): ExecutionReport = {
    val file = reportFile(projectId, taskId, time)
    if(!file.exists) {
      throw new NoSuchElementException(s"No report found for project $projectId and task $taskId at $time.")
    }

    val inputStream = new FileInputStream(file)
    try {
      implicit val rc: ReadContext = ReadContext()
      ExecutionReportSerializers.ExecutionReportJsonFormat.read(Json.parse(inputStream))
    } finally {
      inputStream.close()
    }

  }

  override def addReport(projectId: Identifier, taskId: Identifier, report: ExecutionReport): Unit = {
    implicit val wc = WriteContext[JsValue]()
    val reportJson = ExecutionReportSerializers.ExecutionReportJsonFormat.write(report)

    Files.write(reportFile(projectId, taskId, Instant.now).toPath, Json.prettyPrint(reportJson).getBytes(StandardCharsets.UTF_8))
  }

  def reportFile(projectId: Identifier, taskId: Identifier, time: Instant): File = {
    val fileName = s"${projectId}_${taskId}_${timeFormat.format(time)}.json"
    new File(reportDirectory, fileName)
  }

  def fromReportFile(file: File): Option[ReportMetaData] = {
    val name = file.getName
    if(name.endsWith(".json")) {
      val parts = name.stripSuffix(".json").split('_')
      if(parts.length == 3) {
        for(time <- Try(Instant.from(timeFormat.parse(parts(2)))).toOption) yield {
          ReportMetaData(
            projectId = parts(0),
            taskId = parts(1),
            time = time
          )
        }
      } else {
        None
      }
    } else {
      None
    }
  }

}
