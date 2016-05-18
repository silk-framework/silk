package org.silkframework.runtime.serialization

import scala.reflect.ClassTag
import scala.xml.Node

/**
 * XML serialization format.
 */
abstract class XmlFormat[T: ClassTag] extends SerializationFormat[T, Node] {

  def mimeTypes = Set("text/xml", "application/xml")

  def serializedType = {
    implicitly[ClassTag[T]].runtimeClass
  }

  def format(value: T, mimeType: String)(implicit writeContext: WriteContext[Node]): String = {
    val printer = new scala.xml.PrettyPrinter(120, 2)
    val node = write(value)
    printer.format(node)
  }

}