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