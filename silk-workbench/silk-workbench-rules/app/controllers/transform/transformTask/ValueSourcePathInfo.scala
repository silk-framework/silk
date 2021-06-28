package controllers.transform.transformTask

import play.api.libs.json.{Format, Json}

/**
  * Source value path information.
  *
  * @param path          The path string serialization.
  * @param pathType      Either "value" or "object".
  * @param alreadyMapped If the source path is already used in a mapping.
  * @param objectInfo    Optional information for object value paths, e.g. stats, sub paths.
  */
case class ValueSourcePathInfo(path: String,
                               pathType: String,
                               alreadyMapped: Boolean,
                               objectInfo: Option[ObjectValueSourcePathInfo])

/** Additional information for object source paths
  *
  * @param dataTypeSubPaths The string representations of the data type sub-paths.
  * @param objectSubPaths   The string representations of the object sub-paths.
  */
case class ObjectValueSourcePathInfo(dataTypeSubPaths: Seq[String],
                                     objectSubPaths: Seq[String])

object ObjectValueSourcePathInfo {
  implicit val objectValueSourcePathInfoFormat: Format[ObjectValueSourcePathInfo] = Json.format[ObjectValueSourcePathInfo]
}

object ValueSourcePathInfo {
  implicit val valueSourcePathInfoFormat: Format[ValueSourcePathInfo] = Json.format[ValueSourcePathInfo]
}