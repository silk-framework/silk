package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
  * A JsonFormat serializes all values based on a Play Json format.
  */
class PlayJsonFormat[T: ClassTag](implicit format: Format[T]) extends JsonFormat[T] {

  override def read(value: JsValue)(implicit readContext: ReadContext): T = {
    Json.fromJson[T](value) match {
      case JsSuccess(value, _) =>
        value
      case JsError(errors) =>
        val errorStrings = errors map { case (path, validationErrors) =>
          "JSON Path \"" + path.toJsonString + "\" with error(s): " + validationErrors.map('"' + _.message + '"').mkString(", ")
        }
        throw new Exception(errorStrings.mkString(", "))
    }
  }

  override def write(value: T)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    Json.toJson(value)
  }
}
