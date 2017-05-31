package org.silkframework.runtime.serialization

import scala.reflect.ClassTag
import scala.xml.{Node, XML}

/**
 * XML serialization format.
 */
abstract class XmlFormat[T: ClassTag] extends SerializationFormat[T, Node] {

  /**
    * The MIME types that can be formatted.
    */
  def mimeTypes = Set("text/xml", "application/xml")

  /**
    * Formats a value as string.
    */
  def toString(value: T, mimeType: String)(implicit writeContext: WriteContext[Node]): String = {
    val printer = new scala.xml.PrettyPrinter(120, 2)
    val node = write(value)
    printer.format(node)
  }

  /**
    * Reads a value from an XML string.
    */
  def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): T = {
    read(XML.loadString(value))
  }

  override def toString(values: Iterable[T], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[Node]): String = {
    containerName match {
      case Some(name) =>
        val sb = new StringBuilder()
        sb.append(s"<$name>")
        for(v <- values) {
          sb.append(toString(v, mimeType))
        }
        sb.append(s"</$name>")
        sb.toString()
      case None =>
        throw new IllegalArgumentException("Must define a container name for serializing traversables to an XML String.")
    }
  }

}