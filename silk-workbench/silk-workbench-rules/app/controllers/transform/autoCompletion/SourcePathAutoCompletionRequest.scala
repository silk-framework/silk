package controllers.transform.autoCompletion

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.workspace.activity.workflow.WorkflowTaskContext
import play.api.libs.json.{Format, Json}


/** A request for source path auto-completion. */

case class SourcePathAutoCompletionRequest(@Schema(description = "The context a project task is executed in.", requiredMode = RequiredMode.NOT_REQUIRED)
                                           taskContext: Option[WorkflowTaskContext])

object SourcePathAutoCompletionRequest {
  implicit val sourcePathAutoCompletionRequestFormat: Format[SourcePathAutoCompletionRequest] = Json.format[SourcePathAutoCompletionRequest]
}
