package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{ResourceManager, EmptyResourceManager}
import scala.xml.Node

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object Serialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T], prefixes: Prefixes = Prefixes.empty): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T], prefixes: Prefixes = Prefixes.empty, resourceLoader: ResourceManager = EmptyResourceManager): T = {
    format.read(node)
  }
}