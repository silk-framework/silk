package controllers.workspaceApi.validation

import play.api.libs.json.{Format, Json}

/**
  * Response for a URI template validation request.
 *
  * @param valid      If the URI template is valid or not.
  * @param parseError If not valid, this contains the parse error details.
  */
case class AutoSuggestValidationResponse(valid: Boolean, parseError: Option[AutoSuggestValidationError])

/** A validation error for a single line input. */
case class AutoSuggestValidationError(message: String, start: Int, end: Int)

object AutoSuggestValidationResponse {
  implicit val autoSuggestValidationErrorFormat: Format[AutoSuggestValidationError] = Json.format[AutoSuggestValidationError]
  implicit val autoSuggestValidationResponseFormat: Format[AutoSuggestValidationResponse] = Json.format[AutoSuggestValidationResponse]
}
