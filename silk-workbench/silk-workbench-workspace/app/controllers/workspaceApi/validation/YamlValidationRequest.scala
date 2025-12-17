package controllers.workspaceApi.validation

import play.api.libs.json.{Format, Json}

/**
  * Request to validate a YAML string.
  *
  * @param yaml           The YAML string that should be validated.
  * @param nestingAllowed If false and expectMap is true, then each map value must be an atomic value.
  * @param expectMap      If true then the root object must be a Map.
  */
case class YamlValidationRequest(yaml: String, nestingAllowed: Option[Boolean], expectMap: Option[Boolean])

object YamlValidationRequest {
  implicit val YamlValidationRequestFormat: Format[YamlValidationRequest] = Json.format[YamlValidationRequest]
}