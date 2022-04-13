package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.JsValue

import scala.reflect.ClassTag

/**
  * A JSON format that is based on another format and transforms all serialized values based on user-defined functions.
  *
  * @param format The base JSON format.
  * @param read Function used to transform all read values.
  * @param write Function used to transform all written values.
  */
private class TransformedJsonFormat[O, T: ClassTag](format: JsonFormat[O], read: O => T, write: T => O) extends JsonFormat[T] {

  override def read(value: JsValue)(implicit readContext: ReadContext): T = {
    read(format.read(value))
  }

  override def write(value: T)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    format.write(write(value))
  }
}

object TransformedJsonFormat {

  /**
    * Adds a map function to all JsonFormats.
    */
  implicit class TransformableJsonFormat[O](format: JsonFormat[O]) {
    def map[T: ClassTag](read: O => T, write: T => O): JsonFormat[T] ={
      new TransformedJsonFormat(format, read, write)
    }
  }

}