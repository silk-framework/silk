package org.silkframework.plugins.dataset.xml

import net.sf.saxon.om.NameChecker
import org.silkframework.dataset.TypedProperty
import org.silkframework.plugins.dataset.hierarchical.{HierarchicalEntityWriter, HierarchicalSink}
import org.silkframework.plugins.dataset.xml.util.IndentingXMLStreamWriter
import org.silkframework.runtime.validation.ValidationException

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import javax.xml.stream.{XMLOutputFactory, XMLStreamWriter}


class XmlEntityWriter(outputStream: OutputStream, template: XmlOutputTemplate) extends HierarchicalEntityWriter {

  private val writer: XMLStreamWriter = {
    val factory = XMLOutputFactory.newInstance()
    factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true)
    new IndentingXMLStreamWriter(factory.createXMLStreamWriter(outputStream, StandardCharsets.UTF_8.name()))
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
      if(template.isRootTemplate && properties.tail.isEmpty) {
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
      if(value.size > 1) {
        throw new ValidationException(s"Cannot write multiple attributes for property '$property'.")
      }
      for(v <- value) {
        writeAttribute(property.propertyUri, v)
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
    splitPathUri(uri) match {
      case (None, localName) =>
        writer.writeStartElement(localName)
      case (Some(namespace), localName) =>
        writer.writeStartElement(namespace, localName)
    }
  }

  /**
    * Sets an attribute on a node using a URI.
    */
  private def writeAttribute(uri: String, value: String): Unit = {
    splitPathUri(uri) match {
      case (None, localName) =>
        writer.writeAttribute(localName, value)
      case (Some(namespace), localName) =>
        writer.writeAttribute(namespace, localName, value)
    }
  }

  /**
    * Splits a path URI into a namespace and local name.
    */
  private def splitPathUri(uri: String): (Option[String], String) = {
    val separatorIndex = uri.lastIndexWhere(c => c == '/' || c == '#' || c == ':')
    if(separatorIndex == -1) {
      if (!isValidLocalName(uri)) {
        throw new ValidationException(s"Path '$uri' is not a valid XML NCName. The reason could be that the local name starts with a number.")
      }
      (None, uri)
    } else {
      val namespace = uri.substring(0, separatorIndex + 1)
      val localName = uri.substring(separatorIndex + 1)
      if (!isValidLocalName(localName)) {
        throw new ValidationException(s"Path '$uri' cannot be converted into a QName in XML. The reason could be that the local name '$localName' starts with a number.")
      }
      addPrefix(namespace)
      (Some(namespace), localName)
    }
  }

  /**
    * Tests if a local name is valid.
    */
  @inline
  private def isValidLocalName(name: String): Boolean = {
    // As Saxon is already part of the dependencies we use their name checker.
    NameChecker.isValidNCName(name)
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
