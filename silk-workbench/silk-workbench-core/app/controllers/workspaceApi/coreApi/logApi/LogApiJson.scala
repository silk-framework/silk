package controllers.workspaceApi.coreApi.logApi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.workbench.logging.LogLine
import play.api.libs.json.{Format, Json, JsonConfiguration, OptionHandlers}

import java.time.Instant

/**
  * A log line as returned by the log retrieval API.
  */
@Schema(description = "A single log line.")
case class LogLineJson(@Schema(description = "Position of this line in the log stream of this instance.", example = "4711")
                       sequence: Long,
                       @Schema(description = "When the line was logged, as ISO-8601 instant.", example = "2026-09-11T08:21:04.512Z")
                       timestamp: String,
                       @Schema(description = "Log level.", example = "WARN")
                       level: String,
                       @Schema(description = "Name of the logger.", example = "org.silkframework.workspace.Workspace")
                       logger: String,
                       @Schema(description = "Thread the line was logged on.", example = "application-akka.actor.default-dispatcher-3", nullable = true)
                       thread: Option[String],
                       @Schema(description = "The log message, possibly truncated.")
                       message: String,
                       @Schema(description = "Rendered exception, if the line carried one.", nullable = true)
                       throwable: Option[String])

object LogLineJson {
  // Absent values are written as null, so the response shape is the same for every line
  implicit val jsonConfig: JsonConfiguration = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
  implicit val format: Format[LogLineJson] = Json.format[LogLineJson]

  def apply(line: LogLine): LogLineJson = {
    LogLineJson(
      sequence = line.sequence,
      timestamp = Instant.ofEpochMilli(line.timestamp).toString,
      level = line.level,
      logger = line.logger,
      thread = line.thread,
      message = line.message,
      throwable = line.throwable
    )
  }
}

/**
  * Response of the log retrieval endpoint.
  */
@Schema(description = "The most recent log lines of the answering instance.")
case class LogTailResponse(@Schema(description = "Epoch millis the response was assembled at.")
                           serverTime: Long,
                           @Schema(description = "Instance that answered. Each instance keeps its own buffer. Changes on every start, so a different value means the sequences started over.", example = "dataintegration-7d9f-x2k-5e3a9c1f2b7d4e60")
                           instanceId: String,
                           @Schema(description = "Oldest sequence still buffered.")
                           firstSequence: Long,
                           @Schema(description = "Highest sequence examined. Pass it as the next 'since' value to poll for new lines.")
                           lastSequence: Long,
                           @Schema(description = "True, if the limit stopped the scan before every buffered line was examined, so more matching lines may exist. " +
                             "With 'since' the unexamined lines are newer, continue with 'lastSequence'. " +
                             "Without 'since' they are older than the returned ones and can be read by paging forward from 'since' = 'firstSequence' - 1.")
                           truncated: Boolean,
                           @Schema(description = "Lines evicted between the requested 'since' and what is still buffered. Anything above zero means the client fell behind.")
                           dropped: Long,
                           @ArraySchema(schema = new Schema(description = "The matching lines, oldest first.", implementation = classOf[LogLineJson]))
                           lines: Seq[LogLineJson])

object LogTailResponse {
  implicit val format: Format[LogTailResponse] = Json.format[LogTailResponse]
}

/**
  * Diagnostics of the log buffer, so that an empty log view can be explained without reading the source.
  */
@Schema(description = "State of the in-memory log buffer.")
case class LogBufferStatus(@Schema(description = "Whether capturing is switched on.")
                           enabled: Boolean,
                           @Schema(description = "Whether the appender is currently installed on the root logger.")
                           attached: Boolean,
                           @Schema(description = "Minimum level captured, applied on top of the configured logger levels.", example = "INFO")
                           captureLevel: String,
                           @Schema(description = "Maximum number of retained lines.")
                           capacity: Int,
                           @Schema(description = "Longest message and stack trace retained. Longer ones are truncated.")
                           maxMessageChars: Int,
                           @Schema(description = "Number of lines currently retained.")
                           size: Int,
                           @Schema(description = "Oldest sequence still retained.")
                           firstSequence: Long,
                           @Schema(description = "Newest sequence retained, or -1 while nothing has been captured.")
                           lastSequence: Long,
                           @Schema(description = "Instance that answered. Changes on every start.")
                           instanceId: String,
                           @ArraySchema(schema = new Schema(description = "Loggers that are never captured.", implementation = classOf[String]))
                           excludedLoggers: Seq[String],
                           @ArraySchema(schema = new Schema(description = "Application loggers configured with additivity=false. Their output never reaches the root logger and is missing from the buffer.", implementation = classOf[String]))
                           nonAdditiveLoggers: Seq[String])

object LogBufferStatus {
  implicit val format: Format[LogBufferStatus] = Json.format[LogBufferStatus]
}
