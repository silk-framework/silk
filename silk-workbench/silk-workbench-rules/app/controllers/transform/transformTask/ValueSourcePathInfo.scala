package controllers.transform.transformTask

import play.api.libs.json.{Format, Json}

/**
  * Source value path information.
  *
  * @param path          The path string serialization.
  * @param pathType      Either "value" or "object".
  * @param alreadyMapped If the source path is already used in a mapping.
  */
case class ValueSourcePathInfo(path: String,
                               pathType: String,
                               alreadyMapped: Boolean)

object ValueSourcePathInfo {
  implicit val valueSourcePathInfoFormat: Format[ValueSourcePathInfo] = Json.format[ValueSourcePathInfo]
}