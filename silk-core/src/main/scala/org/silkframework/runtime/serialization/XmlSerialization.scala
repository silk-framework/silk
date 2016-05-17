package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}

import scala.xml.Node

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object XmlSerialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T], writeContext: WriteContext[Node] = WriteContext[Node]()): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T], readContext: ReadContext): T = {
    format.read(node)
  }
}
