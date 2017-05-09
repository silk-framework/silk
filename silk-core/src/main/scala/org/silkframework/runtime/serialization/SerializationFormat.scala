package org.silkframework.runtime.serialization

import scala.reflect.ClassTag

/**
  * Implementing classes support the serialization and deserialization of a specific type.
  *
  * @tparam T The value type that can be serialized by an implementing class.
  * @tparam U The serialized type. For instance scala.xml.Node for XML serializations.
  */
abstract class SerializationFormat[T: ClassTag, U: ClassTag] {

  /**
    * The type that can be serialized by this format.
    */
  def valueType = {
    implicitly[ClassTag[T]].runtimeClass
  }

  /**
    * The type to which values are serialized to. For instance scala.xml.Node for XML serializations.
    */
  def serializedType = {
    implicitly[ClassTag[U]].runtimeClass
  }

  /**
    * The MIME types that can be formatted.
    */
  def mimeTypes: Set[String]

  /**
    * Deserializes a value.
    */
  def read(value: U)(implicit readContext: ReadContext): T

  /**
    * Serializes a value.
    */
  def write(value: T)(implicit writeContext: WriteContext[U]): U

  /**
    * Formats a value as string.
    */
  def toString(value: T, mimeType: String)(implicit writeContext: WriteContext[U]): String

  /**
    * Formats an iterable of values as string. The optional container name is used for formats where array like values
    * must be named, e.g. XML. This needs to be a valid serialization in the respective data model.
    */
  def toString(value: Iterable[T], mimeType: String, containerName: Option[String] = None)(implicit writeContext: WriteContext[U]): String

  /**
    * Reads a value from a string.
    */
  def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): T

}
