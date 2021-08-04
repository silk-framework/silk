package controllers.workspaceApi.validation

import org.silkframework.entity.paths.PartialParseError
import play.api.libs.json.{Format, Json}

/**
  * Request to validate a source path, e.g. used in transform or linking tasks.
  *
  * @param pathExpression The Silk path expression to be validated.
  */
case class SourcePathValidationRequest(pathExpression: String)

object SourcePathValidationRequest {
  implicit val sourcePathValidationRequestFormat: Format[SourcePathValidationRequest] = Json.format[SourcePathValidationRequest]
}

/**
  * Response for a source path validation request.
  * @param valid      If the path expression id valid or not.
  * @param parseError If not valid, this contains the parse error details.
  */
case class SourcePathValidationResponse(valid: Boolean, parseError: Option[PartialParseError])

object SourcePathValidationResponse {
  implicit val partialParseErrorFormat: Format[PartialParseError] = Json.format[PartialParseError]
  implicit val sourcePathValidationResponseFormat: Format[SourcePathValidationResponse] = Json.format[SourcePathValidationResponse]
}