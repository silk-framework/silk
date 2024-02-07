package controllers.transform.autoCompletion

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.workspace.activity.workflow.{WorkflowTaskContext, WorkflowTaskContextOutputTask}
import play.api.libs.json.{Format, Json}


/** A request for target property auto-completion
  *
  * @param vocabularies A list of vocabulary URIs to take the properties from.
  */

case class TargetPropertyAutoCompleteRequest(@ArraySchema(
                                               schema = new Schema(
                                                 description = "A list of vocabulary URIs to take the properties from.",
                                                 implementation = classOf[String]
                                             ))
                                             vocabularies: Option[Seq[String]],
                                             @Schema(description = "The context a project task is executed in.", requiredMode = RequiredMode.NOT_REQUIRED)
                                             taskContext: Option[WorkflowTaskContext])

object TargetPropertyAutoCompleteRequest {
  implicit val targetPropertyAutoCompleteRequestFormat: Format[TargetPropertyAutoCompleteRequest] = Json.format[TargetPropertyAutoCompleteRequest]
}