package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.{Format, JsValue, Json}

import scala.reflect.ClassTag

/**
  * A JsonFormat serializes all values based on a Play Json format.
  */
class PlayJsonFormat[T: ClassTag](implicit format: Format[T]) extends JsonFormat[T] {

  override def read(value: JsValue)(implicit readContext: ReadContext): T = {
    Json.fromJson[T](value).get
  }

  override def write(value: T)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    Json.toJson(value)
  }
}
