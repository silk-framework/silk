package controllers.workspaceApi.coreApi.logApi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Format, Json}

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
