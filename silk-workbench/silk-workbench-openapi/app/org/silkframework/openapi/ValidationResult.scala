package org.silkframework.openapi

import play.api.libs.json.{Json, OWrites}

case class ValidationResult(messages: Seq[String])

object ValidationResult {

  implicit val write: OWrites[ValidationResult] = Json.writes[ValidationResult]

}
