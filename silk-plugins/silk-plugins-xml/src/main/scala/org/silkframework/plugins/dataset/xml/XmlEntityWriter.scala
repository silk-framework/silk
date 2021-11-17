package org.silkframework.plugins.dataset.xml

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import org.silkframework.dataset.TypedProperty
import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.runtime.validation.ValidationException

import java.io.OutputStream
import javax.xml.stream.{XMLOutputFactory, XMLStreamWriter}


class XmlEntityWriter(outputStream: OutputStream, template: XmlOutputTemplate) extends HierarchicalEntityWriter {

  // TODO replace IndentingXMLStreamWriter with class that is not from com.sun package
  private val writer: XMLStreamWriter = {
    val factory = XMLOutputFactory.newInstance()
    factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true)
    new IndentingXMLStreamWriter(factory.createXMLStreamWriter(outputStream))
  }

  // Counts all generated namespace prefixes
  private var prefixCounter = 0

  // Indicates if the first root element has been written already
  private var rootElementWritten: Boolean = false

  // All properties in the path between the root and the current element
  private var properties: List[String] = List[String](template.rootElementName)

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
    // Make sure that the output template allows multiple root entities
    if(rootElementWritten) {
      if(template.isSingleInstruction && properties.tail.isEmpty) {
        throw new ValidationException("Cannot insert more than one element at document root. Your output template definition only allows one entity.")
      }
    } else {
      rootElementWritten = true
    }
    // The property URI might be empty (i.e., object mapping with empty target path) in which case the values should be attached to the parent
    val property = properties.head
    if(property.nonEmpty) {
      writer.writeStartElement(property)
    }
  }

  /**
    * Called after all properties of the current entity have been written.
    */
  override def endEntity(): Unit = {
    val property = properties.head
    if(property.nonEmpty) {
      writer.writeEndElement()
    }
  }

  /**
    * Adds a new property.
    * Must be followed by either [[writeValue]] for writing literal values or [[startEntity]] for writing object values.
    */
  override def startProperty(property: TypedProperty, numberOfValues: Int): Unit = {
    properties ::= property.propertyUri
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
    } else if(property.propertyUri != HierarchicalSink.RDF_TYPE) {
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
      val prefix = uri.substring(0, separatorIndex + 1)
      addPrefix(prefix)
      writer.writeStartElement(prefix, uri.substring(separatorIndex + 1))
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
      addPrefix(prefix)
      writer.writeAttribute(prefix, uri.substring(separatorIndex + 1), value)
    }
  }

  /**
    * Adds a prefix, if it has not been registered already.
    * Generates a readable name.
    */
  private def addPrefix(prefix: String): Unit = {
    if(writer.getPrefix(prefix) == null) {
      writer.setPrefix("ns" + prefixCounter, prefix)
      prefixCounter += 1
    }
  }
}
