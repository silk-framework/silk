package controllers.workspaceApi.uriPattern

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
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
                               required = true,
                               implementation = classOf[String])
                             projectId: String)

object UriPatternRequest {
  implicit val uriPatternRequestFormat: Format[UriPatternRequest] = Json.format[UriPatternRequest]
}
