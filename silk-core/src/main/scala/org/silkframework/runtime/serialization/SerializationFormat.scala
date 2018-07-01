package org.silkframework.runtime.serialization

import scala.collection.mutable
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
  def valueType: Class[_] = implicitly[ClassTag[T]].runtimeClass

  /**
    * The type to which values are serialized to. For instance scala.xml.Node for XML serializations.
    */
  def serializedType: Class[_] = implicitly[ClassTag[U]].runtimeClass

  /**
    * The MIME types that can be formatted.
    */
  def mimeTypes: Set[String]

  def read(value: U)(implicit readContext: ReadContext): T

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

  /**
    * Read Serialization format from string
    */
  def parse(value: String, mimeType: String): U


  // register the new serializer
  SerializationFormat.registerSerializationFormat(this)
}

object SerializationFormat{

  /* Serializer Registry */

  private val SerializerRegistry = new mutable.HashMap[(ClassTag[_], ClassTag[_]), SerializationFormat[_, _]]

  def registerSerializationFormat(sf: SerializationFormat[_, _])(): Unit ={
    //add entry for each mime type
    SerializerRegistry.put((ClassTag(sf.valueType), ClassTag(sf.serializedType)), sf)
  }

  def getSerializationFormat[Typ, Ser](implicit typTag: ClassTag[_], serTag: ClassTag[_]): Option[SerializationFormat[Typ, Ser]] ={
    SerializerRegistry.get((typTag, serTag)).asInstanceOf[Option[SerializationFormat[Typ, Ser]]]
  }

  def listAllSerializers: List[SerializationFormat[_, _]] = SerializerRegistry.values.toList.distinct
}