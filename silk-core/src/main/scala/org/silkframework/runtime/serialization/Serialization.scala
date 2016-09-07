package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry

import scala.reflect.ClassTag
import scala.xml.Node

/**
  * Utility class for serializing values.
  */
object Serialization {

  private lazy val serializationFormats: Seq[SerializationFormat[Any, Any]] = {
    implicit val prefixes = Prefixes.empty
    val formatTypes = PluginRegistry.availablePlugins[SerializationFormat[Any, Any]]
    formatTypes.map(_.apply())
  }

  def hasSerialization(value: Any, mimeType: String): Boolean = {
    serializationFormats.exists(f => f.serializedType == value.getClass && f.mimeTypes.contains(mimeType))
  }

  def serialize(value: Any, mimeType: String): String = {
    implicit val writeContext = WriteContext[Any]()
    find(value.getClass, mimeType).format(value, mimeType)
  }

  def deserialize[T: ClassTag](value: String, mimeType: String): T = {
    implicit val readContext = ReadContext()
    val valueType = implicitly[ClassTag[T]].runtimeClass
    find(valueType, mimeType).fromString(value, mimeType).asInstanceOf[T]
  }

  private def find(valueType: Class[_], mimeType: String) = {
    serializationFormats.find(f => f.serializedType == valueType && f.mimeTypes.contains(mimeType)) match {
      case Some(format) =>
        format
      case None =>
        throw new NoSuchElementException(s"No serialization format for type $valueType for content type $mimeType available.")
    }
  }

}
