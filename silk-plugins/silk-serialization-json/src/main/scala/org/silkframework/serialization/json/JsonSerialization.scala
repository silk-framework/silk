package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.JsValue

/**
  * Serializes between classes and JSON.
  * In order to be serializable a class needs to provide an implicit JsonFormat object.
  */
object JsonSerialization {

  def toJson[T](value: T)(implicit format: JsonFormat[T], writeContext: WriteContext[JsValue] = WriteContext[JsValue](projectId = None)): JsValue = {
    format.write(value)
  }

  def fromJson[T](node: JsValue)(implicit format: JsonFormat[T], readContext: ReadContext): T = {
    format.read(node)
  }
}
