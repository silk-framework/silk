package org.silkframework.plugins.dataset.xml

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import org.silkframework.dataset.TypedProperty
import org.silkframework.plugins.dataset.hierarchical.HierarchicalEntityWriter
import org.silkframework.runtime.validation.ValidationException

import java.io.OutputStream
import javax.xml.stream.{XMLOutputFactory, XMLStreamWriter}

//TODO add maximum nesting level
class XmlEntityWriter(outputStream: OutputStream, template: XmlTemplate) extends HierarchicalEntityWriter {

  // TODO replace IndentingXMLStreamWriter with class that is not from com.sun package
  private val writer: XMLStreamWriter = {
    val factory = XMLOutputFactory.newInstance()
    factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true)
    new IndentingXMLStreamWriter(factory.createXMLStreamWriter(outputStream))
  }

  private var prefixCounter = 0

  private var level: Int = 0

  private var properties: List[String] = List[String]()

  /**
    * Open this writer.
    * Must be called once at the beginning.
    */
  override def open(singleRootEntity: Boolean): Unit = {
    template.writePrefix(writer)
  }

  /**
    * Adds a new entity.
    * Must be followed by calls to [[startProperty]] to write property values.
    */
  override def startEntity(): Unit = {
    if(level == 0) {
      writer.writeStartElement(template.rootElementName)
    } else {
      //TODO property uri could be empty
      writer.writeStartElement(properties.head)
    }
    level += 1
  }

  /**
    * Called after all properties of the current entity have been written.
    */
  override def endEntity(): Unit = {
    writer.writeEndElement()
    level -= 1
  }

  /**
    * Adds a new property.
    * Must be followed by either [[writeValue]] for writing literal values or [[startEntity]] for writing object values.
    */
  override def startProperty(property: TypedProperty, numberOfValues: Int): Unit = {
    properties ::= property.propertyUri
    if(property.isAttribute) {

    } else {
    }
  }

  /**
    * Called after all values of the current property have been written.
    */
  override def endProperty(property: TypedProperty): Unit = {
    properties = properties.tail
  }

  /**
    * Writes a literal value.
    */
  override def writeValue(value: Seq[String], property: TypedProperty): Unit = {
    if(property.isAttribute) {
      value match {
        case Seq(v) =>
          writeAttribute(property.propertyUri, v)
        case _ =>
          throw new ValidationException(s"Cannot write multiple attributes for property '$property'.")
      }
    } else {
      for(v <- value) {
        if(property.propertyUri == "#text") {
          writer.writeCharacters(v)
        } else {
          writeStartElement(property.propertyUri)
          writer.writeCharacters(v)
          writer.writeEndElement()
        }
      }
    }
  }

  /**
    * Closes this writer and releases resources.
    */
  override def close(): Unit = {
    try {
      template.writeSuffix(writer)
    } finally {
      writer.close()
    }
  }

  /**
    * Generates an empty XML element from a URI.
    */
  private def writeStartElement(uri: String): Unit = {
    val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#')
    if(separatorIndex == -1) {
      writer.writeStartElement(uri)
    } else {
      writer.writeStartElement(uri.substring(0, separatorIndex + 1), uri.substring(separatorIndex + 1))
    }
  }

  /**
    * Sets an attribute on a node using a URI.
    */
  private def writeAttribute(uri: String, value: String): Unit = {
    val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#')
    if(separatorIndex == -1) {
      writer.writeAttribute(uri, value)
    } else {
      val prefix = uri.substring(0, separatorIndex + 1)
      if(writer.getPrefix(prefix) == null) {
        writer.setPrefix("ns" + prefixCounter, prefix)
        prefixCounter += 1
      }
      writer.writeAttribute(prefix, uri.substring(separatorIndex + 1), value)
    }
  }
}
