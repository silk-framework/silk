package controllers.workspaceApi.coreApi.logApi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Format, Json, JsonConfiguration, OptionHandlers}

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
                           @Schema(description = "When the oldest retained line was logged, as ISO-8601 instant, or null while nothing has been captured.", example = "2026-09-11T08:21:04.512Z", nullable = true)
                           firstTimestamp: Option[String],
                           @Schema(description = "When the newest retained line was logged, as ISO-8601 instant, or null while nothing has been captured.", example = "2026-09-11T09:47:15.031Z", nullable = true)
                           lastTimestamp: Option[String],
                           @Schema(description = "Instance that answered. Changes on every start.")
                           instanceId: String,
                           @ArraySchema(schema = new Schema(description = "Loggers that are never captured.", implementation = classOf[String]))
                           excludedLoggers: Seq[String],
                           @ArraySchema(schema = new Schema(description = "Application loggers configured with additivity=false. Their output never reaches the root logger and is missing from the buffer.", implementation = classOf[String]))
                           nonAdditiveLoggers: Seq[String])

object LogBufferStatus {
  implicit val jsonConfig: JsonConfiguration = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
  implicit val format: Format[LogBufferStatus] = Json.format[LogBufferStatus]
}
