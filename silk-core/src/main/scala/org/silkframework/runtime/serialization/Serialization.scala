package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry

import scala.xml.Node

/**
  * Utility class for serializing values.
  * Currently supports XML.
  */
object Serialization {

  private lazy val serializationFormats: Seq[XmlFormat[Any]] = {
    implicit val prefixes = Prefixes.empty
    val formatTypes = PluginRegistry.availablePlugins[XmlFormat[Any]]
    formatTypes.map(_.apply())
  }

  private val printer = new scala.xml.PrettyPrinter(120, 2)

  def serialize(value: Any): String = {
    implicit val writeContext = WriteContext[Node]()
    serializationFormats.find(_.serializedType == value.getClass) match {
      case Some(format) =>
        val node = format.write(value)
        printer.format(node)
      case None =>
        throw new NoSuchElementException(s"No XML serialization format for type ${value.getClass} available.")
    }
  }

}
