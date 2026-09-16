package controllers.workspaceApi.coreApi

import controllers.workspaceApi.coreApi.logs.{LogBufferStatus, LogLineJson, LogQuery, LogTailResponse}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.validation.ServiceUnavailableException
import org.silkframework.workbench.logging.{LogBuffer, LogStore}
import org.silkframework.workbench.utils.ErrorResult.ErrorResultFormat
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

import java.net.InetAddress
import javax.inject.Inject
import scala.util.Try

/**
  * Retrieval of the most recent log lines of this instance.
  *
  * Exists for administrators who need to find out why an action failed but have no access to the container.
  * Polling with 'since' gives a live view while an action is being reproduced.
  * Log lines can contain data of all users, so the endpoints are restricted to administrators by the access control
  * configuration of the deployment, like the other administrative endpoints.
  */
@Tag(name = "Workbench")
class LogApi @Inject()(logBuffer: LogBuffer) extends InjectedController {

  @Operation(
    summary = "Get the most recent log lines of this instance",
    description = LogApi.tailDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The matching log lines, oldest first.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[LogTailResponse])
        ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If a filter is not valid.",
        content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[ErrorResultFormat])))
      ),
      new ApiResponse(
        responseCode = "503",
        description = "If the log buffer is disabled on this instance.",
        content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[ErrorResultFormat])))
      )
    ))
  def logTail(@Parameter(
                name = "since",
                description = "Return only lines with a higher sequence than this. Pass the 'lastSequence' of the previous response to poll for new lines.",
                required = false,
                in = ParameterIn.QUERY,
                schema = new Schema(implementation = classOf[Long])
              )
              since: Option[Long],
              @Parameter(
                name = "limit",
                description = "Maximum number of lines to return, 200 by default and at most 2000.",
                required = false,
                in = ParameterIn.QUERY,
                schema = new Schema(implementation = classOf[Int])
              )
              limit: Option[Int],
              @Parameter(
                name = "level",
                description = "Minimum log level a line has to be logged at.",
                required = false,
                in = ParameterIn.QUERY,
                schema = new Schema(implementation = classOf[String], allowableValues = Array("TRACE", "DEBUG", "INFO", "WARN", "ERROR"))
              )
              level: Option[String],
              @Parameter(
                name = "logger",
                description = "Return only lines of loggers starting with one of these prefixes.",
                required = false,
                in = ParameterIn.QUERY,
                array = new ArraySchema(schema = new Schema(implementation = classOf[String]))
              )
              logger: List[String],
              @Parameter(
                name = "contains",
                description = "Return only lines containing all of these substrings in the message or exception, ignoring case.",
                required = false,
                in = ParameterIn.QUERY,
                array = new ArraySchema(schema = new Schema(implementation = classOf[String]))
              )
              contains: List[String]): Action[AnyContent] = Action {
    val store = availableStore()
    val query = LogQuery(level, logger, contains, limit)
    val page = since match {
      case Some(sinceSequence) => store.since(sinceSequence, query.limit, query.matches)
      case None => store.last(query.limit, query.matches)
    }
    val firstSequence = store.firstSequence
    Ok(Json.toJson(LogTailResponse(
      serverTime = System.currentTimeMillis(),
      instanceId = LogApi.instanceId,
      firstSequence = firstSequence,
      lastSequence = page.nextCursor,
      truncated = page.truncated,
      dropped = droppedSince(since, firstSequence),
      lines = page.lines.map(LogLineJson(_))
    )))
  }

  @Operation(
    summary = "Get the state of the in-memory log buffer",
    description = "Reports whether log lines are being captured, at which level, and which loggers are excluded. " +
      "Use this to find out why the log retrieval API returns fewer lines than expected, or none at all.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The buffer state.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[LogBufferStatus])
        ))
      )
    ))
  def logBufferStatus(): Action[AnyContent] = Action {
    val config = logBuffer.config
    val store = logBuffer.store
    Ok(Json.toJson(LogBufferStatus(
      enabled = config.enabled,
      attached = logBuffer.isAttached,
      captureLevel = config.level,
      capacity = config.capacity,
      maxMessageChars = config.maxMessageChars,
      size = store.map(_.size).getOrElse(0),
      firstSequence = store.map(_.firstSequence).getOrElse(0L),
      lastSequence = store.map(_.lastSequence).getOrElse(-1L),
      instanceId = LogApi.instanceId,
      excludedLoggers = config.excludedLoggers,
      nonAdditiveLoggers = logBuffer.nonAdditiveLoggers
    )))
  }

  /** Lines lost between what the client last saw and what is still buffered. Without it, a gap looks like a quiet period. */
  private def droppedSince(since: Option[Long], firstSequence: Long): Long = {
    since.filter(_ >= 0).map(s => math.max(0L, firstSequence - s - 1)).getOrElse(0L)
  }

  private def availableStore(): LogStore = {
    logBuffer.store.getOrElse {
      throw ServiceUnavailableException("The in-memory log buffer is not active on this instance. Set 'logging.buffer.enabled' " +
        "to true and restart. See the 'logs/status' endpoint for the current state.")
    }
  }
}

object LogApi {

  /** Identifies the answering instance, e.g. the container name. */
  lazy val instanceId: String = Try(InetAddress.getLocalHost.getHostName).getOrElse("unknown")

  // No type ascription, so the value stays a compile-time constant usable in the annotation
  final val tailDescription =
    "Returns the most recent log lines held by the instance answering this request.\n\n" +
      "**Following the log**\n\n" +
      "Poll this endpoint and pass the `lastSequence` of the previous response as `since` to receive only what has been " +
      "logged since. A `dropped` above zero means the client fell behind and those lines are no longer buffered.\n\n" +
      "**Limitations**\n\n" +
      "| Limitation | Detail |\n" +
      "| ---------- | ------ |\n" +
      "| Per instance | Each instance keeps its own buffer and answers only with its own lines. |\n" +
      "| Bounded history | Only the most recent lines are kept, see `logging.buffer.capacity`. |\n" +
      "| Level | Only lines at or above `logging.buffer.level` are captured, regardless of the logger levels. |\n" +
      "| Excluded loggers | Loggers listed in `logging.buffer.excludedLoggers` are never captured. |\n\n" +
      "Use the `logs/status` endpoint to see the current settings and whether capturing is active at all."
}
