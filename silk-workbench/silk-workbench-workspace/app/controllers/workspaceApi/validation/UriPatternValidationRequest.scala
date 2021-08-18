package controllers.workspaceApi.validation

import play.api.libs.json.{Format, Json}

/**
  * Request to validate a URI pattern, e.g. used in transform or linking tasks.
  *
  * @param uriPattern The string representation of the URI pattern.
  */
case class UriPatternValidationRequest(uriPattern: String)

object UriPatternValidationRequest {
  implicit val UriTemplateValidationRequestFormat: Format[UriPatternValidationRequest] = Json.format[UriPatternValidationRequest]
}