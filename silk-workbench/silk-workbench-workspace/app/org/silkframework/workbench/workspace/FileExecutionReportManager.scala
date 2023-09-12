package org.silkframework.workbench.workspace

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneOffset}
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.ActivityExecutionResult
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ActivitySerializers.ActivityExecutionResultJsonFormat
import org.silkframework.serialization.json.ExecutionReportSerializers.ExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.reports.ExecutionReportManager.DEFAULT_RETENTION_TIME
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}
import play.api.libs.json.{JsValue, Json}

import scala.collection.immutable.ArraySeq
import scala.util.Try

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

  override def listReports(projectId: Option[Identifier], taskId: Option[Identifier]): Seq[ReportIdentifier] = synchronized {
    for {
      reportFile <- ArraySeq.unsafeWrapArray(reportDirectory.listFiles())
      report <- fromReportFile(reportFile)
      if projectId.forall(_ == report.projectId)
      if taskId.forall(_ == report.taskId)
    } yield {
     report
    }
  }

  override def retrieveReport(reportId: ReportIdentifier)
                             (implicit pluginContext: PluginContext): ActivityExecutionResult[ExecutionReport] = synchronized {
    val file = reportFile(reportId)
    if(!file.exists) {
      throw new NoSuchElementException(s"No report found for project ${reportId.projectId} and task ${reportId.taskId} at ${reportId.time}.")
    }

    val inputStream = new FileInputStream(file)
    try {
      implicit val rc: ReadContext = ReadContext.fromPluginContext()(pluginContext)
      reportJsonFormat.read(Json.parse(inputStream))
    } finally {
      inputStream.close()
    }

  }

  override def addReport(reportId: ReportIdentifier, report: ActivityExecutionResult[ExecutionReport])
                        (implicit pluginContext: PluginContext): Unit = synchronized {
    implicit val wc: WriteContext[JsValue] = WriteContext.fromPluginContext[JsValue]()(pluginContext)
    val reportJson = reportJsonFormat.write(report)

    removeOldReports(retentionTime)
    Files.write(reportFile(reportId).toPath, Json.prettyPrint(reportJson).getBytes(StandardCharsets.UTF_8))
  }

  override def removeReport(reportId: ReportIdentifier): Unit = synchronized {
    val file = reportFile(reportId)
    Files.delete(file.toPath)
  }

  /**
    * Returns the file that corresponds to a given report identifier.
    */
  private def reportFile(reportId: ReportIdentifier): File = {
    val fileName = s"${reportId.projectId}+${reportId.taskId}+${timeFormat.format(reportId.time)}.json"
    new File(reportDirectory, fileName)
  }

  /**
    * Parses report identifier from a file name.
    */
  private def fromReportFile(file: File): Option[ReportIdentifier] = {
    val name = file.getName
    if(name.endsWith(".json")) {
      val parts = name.stripSuffix(".json").split('+')
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

