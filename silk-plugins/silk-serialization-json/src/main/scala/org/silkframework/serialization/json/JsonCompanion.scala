package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.{Format, JsValue, Json}

/**
  * A companion object for a data class that can be serialized to and from JSON.
  * This is preferred to implementing a JsonFormat directly, since it also allows to annotate the JSON class with OpenAPI annotations and generate the JSON schema.
  *
  * @tparam DataClass The data class that is serialized to and from JSON.
  * @tparam JsonClass The JSON class that is used for serialization.
  */
trait JsonCompanion[DataClass, JsonClass] {

  implicit def jsonFormat: Format[JsonClass]

  def read(json: JsonClass)(implicit readContext: ReadContext): DataClass

  def write(data: DataClass)(implicit writeContext: WriteContext[JsValue]): JsonClass

  def readJson(json: JsValue)(implicit readContext: ReadContext): DataClass = {
    read(Json.fromJson[JsonClass](json).get)
  }

  def writeJson(data: DataClass)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    Json.toJson(write(data))
  }
}