package controllers.workspaceApi.uriPattern

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import play.api.libs.json.{Format, Json}

/** Request for finding URI patterns. */
case class UriPatternRequest(@ArraySchema(
                               schema = new Schema(
                                 description = "The target class URIs to find URI pattern suggestions for. These can be absolute or prefixed URIs.",
                                 implementation = classOf[String]
                              ))
                             targetClassUris: Seq[String],
                             @Schema(
                               description = "The project ID.",
                               requiredMode = RequiredMode.REQUIRED,
                               implementation = classOf[String])
                             projectId: String,
                             @Schema(
                               description = "When unique values are requested each URI pattern will only appear once even when found for different type URIs.",
                               requiredMode = RequiredMode.NOT_REQUIRED,
                               defaultValue = "false",
                               implementation = classOf[Boolean])
                             uniqueValues: Option[Boolean] = Some(false))

object UriPatternRequest {
  implicit val uriPatternRequestFormat: Format[UriPatternRequest] = Json.format[UriPatternRequest]
}
