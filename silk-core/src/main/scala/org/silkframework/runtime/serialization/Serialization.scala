package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry

import scala.reflect.ClassTag

/**
  * Utility class for serializing values.
  */
object Serialization {

  private lazy val serializationFormats: Seq[SerializationFormat[Any, Any]] = {
    implicit val prefixes = Prefixes.empty
    val formatTypes = PluginRegistry.availablePlugins[SerializationFormat[Any, Any]]
    formatTypes.map(_.apply())
  }

  def hasSerialization[T: ClassTag](mimeType: String): Boolean = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    serializationFormats.exists(f => f.valueType == valueType && (f.mimeTypes.contains(mimeType) || mimeType == "*/*"))
  }

  def formatForType[T: ClassTag, U: ClassTag]: SerializationFormat[T, U] = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    val serializedType = implicitly[ClassTag[U]].runtimeClass
    serializationFormats.find(f => f.valueType == valueType && f.serializedType == serializedType) match {
      case Some(format) =>
        format.asInstanceOf[SerializationFormat[T, U]]
      case None =>
        throw new NoSuchElementException(s"No serialization format for type $valueType for serialization type $serializedType available.")
    }
  }

  def formatForMime[T: ClassTag](mimeType: String): SerializationFormat[T, Any] = {
    formatForMime(implicitly[ClassTag[T]].runtimeClass, mimeType).asInstanceOf[SerializationFormat[T, Any]]
  }

  def formatForMime(valueType: Class[_], mimeType: String): SerializationFormat[Any, Any] = {
    serializationFormats.find(f => f.valueType == valueType && (f.mimeTypes.contains(mimeType) || mimeType == "*/*")) match {
      case Some(format) =>
        format
      case None =>
        throw new NoSuchElementException(s"No serialization format for type $valueType for content type $mimeType available.")
    }
  }

}
