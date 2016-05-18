package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry

import scala.xml.Node

/**
  * Utility class for serializing values.
  */
object Serialization {

  private lazy val serializationFormats: Seq[XmlFormat[Any]] = {
    implicit val prefixes = Prefixes.empty
    val formatTypes = PluginRegistry.availablePlugins[XmlFormat[Any]]
    formatTypes.map(_.apply())
  }

  private val printer = new scala.xml.PrettyPrinter(120, 2)

  def hasSerialization(value: Any, mimeType: String): Boolean = {
    serializationFormats.exists(f => f.serializedType == value.getClass && f.mimeTypes.contains(mimeType))
  }

  def serialize(value: Any, mimeType: String): String = {
    implicit val writeContext = WriteContext[Node]()
    serializationFormats.find(f => f.serializedType == value.getClass && f.mimeTypes.contains(mimeType)) match {
      case Some(format) =>
        format.format(value, mimeType)
      case None =>
        throw new NoSuchElementException(s"No serialization format for type ${value.getClass} for content type $mimeType available.")
    }
  }

}
