package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
import play.api.libs.json.{JsValue, Json}

import scala.reflect.ClassTag
import scala.xml.Node

/**
  * JSON serialization format.
  */
abstract class JsonFormat[T: ClassTag] extends SerializationFormat[T, JsValue] {

  /**
    * The MIME types that can be formatted.
    */
  def mimeTypes = Set("application/json")

  /**
    * Formats a JSON value as string.
    */
  def toString(value: T, mimeType: String)(implicit writeContext: WriteContext[JsValue]): String = {
    Json.stringify(write(value))
  }

  /**
    * Reads a value from a JSON string.
    */
  def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): T = {
    read(Json.parse(value))
  }

  override def toString(values: Iterable[T], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[JsValue]): String = {
    val sb = new StringBuilder()
    sb.append(s"[")
    for((v, idx) <- values.zipWithIndex) {
      if(idx > 0) {
        sb.append(",")
      }
      sb.append(toString(v, mimeType))
    }
    sb.append(s"]")
    sb.toString()
  }
}
