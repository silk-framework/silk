package org.silkframework.workbench.workspace

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneOffset}
import java.util.logging.{Level, Logger}

import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ActivitySerializers.ActivityExecutionResultJsonFormat
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workbench.workspace.FileExecutionReportManager.DEFAULT_RETENTION_TIME
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.{JsValue, Json}

import scala.util.Try
import scala.util.control.NonFatal

@Plugin(
  id = "file",
  label = "Reports on filesystem",
  description = "Holds the reports in a specified directory on the filesystem."
)
case class FileExecutionReportManager(dir: String, retentionTime: Duration = DEFAULT_RETENTION_TIME) extends ExecutionReportManager {

  private val reportDirectory = new File(dir)
  reportDirectory.mkdirs()

  // Time format to encode times in file names
  private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(ZoneOffset.UTC)

  // JSON format to read and write execution reports.
  private val reportJsonFormat = new ActivityExecutionResultJsonFormat()(ExecutionReportJsonFormat)

  private val log = Logger.getLogger(getClass.getName)

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportIdentifier] = {
    for {
      reportFile <- reportDirectory.listFiles()
      report <- fromReportFile(reportFile)
      if projectId.forall(_ == report.projectId)
      if taskId.forall(_ == report.taskId)
    } yield {
     report
    }
  }

  override def retrieveReport(reportId: ReportIdentifier): ActivityExecutionResult[ExecutionReport] = {
    val file = reportFile(reportId)
    if(!file.exists) {
      throw new NoSuchElementException(s"No report found for project ${reportId.projectId} and task ${reportId.taskId} at ${reportId.time}.")
    }

    val inputStream = new FileInputStream(file)
    try {
      implicit val rc: ReadContext = ReadContext()
      reportJsonFormat.read(Json.parse(inputStream))
    } finally {
      inputStream.close()
    }

  }

  override def addReport(reportId: ReportIdentifier, report: ActivityExecutionResult[ExecutionReport]): Unit = {
    implicit val wc = WriteContext[JsValue]()
    val reportJson = reportJsonFormat.write(report)

    removeOldReports()
    Files.write(reportFile(reportId).toPath, Json.prettyPrint(reportJson).getBytes(StandardCharsets.UTF_8))
  }

  override def removeReport(reportId: ReportIdentifier): Unit = {
    val file = reportFile(reportId)
    Files.delete(file.toPath)
  }

  /**
    * Removes reports older than the retention time.
    * If removal of a report fails, a warning will be logged and the method will return normally.
    */
  private def removeOldReports(): Unit = {
    val oldestDateTime = Instant.now.minus(retentionTime)
      for (reportId <- listReports(None, None) if reportId.time.isBefore(oldestDateTime)) {
        try {
          removeReport(reportId)
        } catch {
          case NonFatal(ex) =>
            log.log(Level.WARNING, s"Could not delete old report file at " + reportFile(reportId).getAbsolutePath, ex)
        }
      }

  }

  /**
    * Returns the file that corresponds to a given report identifier.
    */
  private def reportFile(reportId: ReportIdentifier): File = {
    val fileName = s"${reportId.projectId}_${reportId.taskId}_${timeFormat.format(reportId.time)}.json"
    new File(reportDirectory, fileName)
  }

  /**
    * Parses report identifier from a file name.
    */
  private def fromReportFile(file: File): Option[ReportIdentifier] = {
    val name = file.getName
    if(name.endsWith(".json")) {
      val parts = name.stripSuffix(".json").split('_')
      if(parts.length == 3) {
        for(time <- Try(Instant.from(timeFormat.parse(parts(2)))).toOption) yield {
          ReportIdentifier(
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

object FileExecutionReportManager {

  val DEFAULT_RETENTION_TIME: Duration = Duration.ofDays(30)

}
