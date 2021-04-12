package controllers.transform.autoCompletion

import play.api.libs.json.{Format, Json}


/** A request for target property auto-completion
  *
  * @param vocabularies A list of vocabulary URIs to take the properties from.
  */
case class TargetPropertyAutoCompleteRequest(vocabularies: Option[Seq[String]])

object TargetPropertyAutoCompleteRequest {
  implicit val targetPropertyAutoCompleteRequestFormat: Format[TargetPropertyAutoCompleteRequest] = Json.format[TargetPropertyAutoCompleteRequest]
}