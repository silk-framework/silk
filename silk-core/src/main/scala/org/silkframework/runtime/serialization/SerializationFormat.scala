package org.silkframework.runtime.serialization

import scala.reflect.ClassTag

/**
  * Implementing classes support the serialization and deserialization of a specific type.
  *
  * @tparam T The value type that can be serialized by an implementing class.
  * @tparam U The serialized type. For instance scala.xml.Node for XML serializations.
  */
abstract class SerializationFormat[T: ClassTag, U] {

  def serializedType = {
    implicitly[ClassTag[T]].runtimeClass
  }

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
  def format(value: T, mimeType: String)(implicit writeContext: WriteContext[U]): String

}
