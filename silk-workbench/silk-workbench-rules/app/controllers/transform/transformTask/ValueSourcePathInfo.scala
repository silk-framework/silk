package controllers.transform.transformTask

import io.swagger.v3.oas.annotations.media.Schema
import play.api.libs.json.{Format, Json}

/**
  * Source value path information.
  *
  * @param path          The path string serialization.
  * @param pathType      Either "value" or "object".
  * @param alreadyMapped If the source path is already used in a mapping.
  * @param objectInfo    Optional information for object value paths, e.g. stats, sub paths.
  */
case class ValueSourcePathInfo(@Schema(
                                 description = "Serialized path representation"
                               )
                               path: String,
                               @Schema(
                                 description = "Either 'value' or 'object' depending the type of values the path points at"
                               )
                               pathType: String,
                               @Schema(
                                 description = "Signals if this path is already in use by another mapping rule in the same transformation."
                               )
                               alreadyMapped: Boolean,
                               @Schema(
                                 description = "Added when the `objectInfo` query parameters has been enabled and the path is of type 'object'.",
                                 required = false
                               )
                               objectInfo: Option[ObjectValueSourcePathInfo])

/** Additional information for object source paths
  *
  * @param dataTypeSubPaths The string representations of the data type sub-paths.
  * @param objectSubPaths   The string representations of the object sub-paths.
  */
case class ObjectValueSourcePathInfo(@Schema(
                                       description = "Direct value paths, i.e. of all direct sub-paths of length 1 of the object values of the path."
                                     )
                                     dataTypeSubPaths: Seq[String],
                                     @Schema(
                                       description = "Direct object paths, i.e. of all direct sub-paths of length 1 of the object values of the path."
                                     )
                                     objectSubPaths: Seq[String])

object ObjectValueSourcePathInfo {
  implicit val objectValueSourcePathInfoFormat: Format[ObjectValueSourcePathInfo] = Json.format[ObjectValueSourcePathInfo]
}

object ValueSourcePathInfo {
  implicit val valueSourcePathInfoFormat: Format[ValueSourcePathInfo] = Json.format[ValueSourcePathInfo]
}