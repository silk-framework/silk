package org.silkframework.openapi

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Json, OWrites}

@Schema(description = "Result of a validation of an OpenAPI specification.")
case class ValidationResult(@ArraySchema(schema = new Schema(implementation = classOf[String]))
                            messages: Seq[String])

object ValidationResult {

  implicit val write: OWrites[ValidationResult] = Json.writes[ValidationResult]

}
