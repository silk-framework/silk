package de.fuberlin.wiwiss.silk.runtime.serialization

import scala.xml.Node

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object Serialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T]): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T]): T = {
    format.read(node)
  }
}