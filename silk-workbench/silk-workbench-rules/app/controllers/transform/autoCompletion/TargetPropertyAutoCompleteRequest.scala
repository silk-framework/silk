package controllers.transform.autoCompletion

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
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
                                             vocabularies: Option[Seq[String]])

object TargetPropertyAutoCompleteRequest {
  implicit val targetPropertyAutoCompleteRequestFormat: Format[TargetPropertyAutoCompleteRequest] = Json.format[TargetPropertyAutoCompleteRequest]
}