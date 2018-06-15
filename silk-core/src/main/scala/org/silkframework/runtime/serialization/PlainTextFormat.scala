package org.silkframework.runtime.serialization

import org.silkframework.runtime.validation.ValidationException

/**
  * A serialization format that serializes any type by calling toString.
  * Throws an exception when trying to deserialize any value.
  */
object PlainTextFormat extends SerializationFormat[Any, String] {
  /**
    * The MIME types that can be formatted.
    */
  override def mimeTypes: Set[String] = Set("text/plain")

  override def read(value: String)(implicit readContext: ReadContext): Any = throwDeserializationException

  override def write(value: Any)(implicit writeContext: WriteContext[String]): String = value.toString

  /**
    * Formats a value as string.
    */
  override def toString(value: Any, mimeType: String)(implicit writeContext: WriteContext[String]): String = value.toString

  /**
    * Formats an iterable of values as string. The optional container name is used for formats where array like values
    * must be named, e.g. XML. This needs to be a valid serialization in the respective data model.
    */
  override def toString(value: Iterable[Any], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[String]): String = {
    value.toString()
  }

  /**
    * Reads a value from a string.
    */
  override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): Any = throwDeserializationException

  private def throwDeserializationException: Nothing = {
    throw new ValidationException("Cannot deserialize values from plain text (text/plain)")
  }
}
