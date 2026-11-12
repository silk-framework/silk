package controllers.workspaceApi.coreApi.logApi

import io.swagger.v3.oas.annotations.media.Schema
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
