package org.silkframework.runtime.serialization

import org.apache.xalan.xsltc.trax.DOM2SAX

import java.io.{InputStream, OutputStream}
import javax.xml.stream.util.StreamReaderDelegate
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}
import javax.xml.transform.Transformer
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.{Document, Element}

import java.rmi.UnexpectedException
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.xml.{Elem, MetaData, Node, PCData, Text, TopScope, UnprefixedAttribute}
import scala.xml.parsing.NoBindingFactoryAdapter

/**
  * Serialization format that serializes Scala objects to XML and back using streaming.
  */
abstract class StreamXmlFormat[T: ClassTag] {
  val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()

  /** Read a value from the input stream */
  def read(implicit streamReader: XMLStreamReader, readContext: ReadContext): T

  /** Write value as XML to an output stream */
  def write(value: T, outputStream: OutputStream): Unit

  def read(inputStream: InputStream)(implicit readContext: ReadContext): T = {
    val xmlInputFactory = XMLInputFactory.newInstance()
    implicit val xmlStreamReader = new StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(inputStream)) {
      override def getVersion: String = "1.0" // This is needed because else this will result in a NullPointerException
    }
    placeOnNextTag()
    read
  }

  def withTag(tag: String)(block: => Unit)(implicit outputStream: OutputStream): Unit = {
    writeStartTag(tag)
    block
    writeEndTag(tag)
  }

  def writeStartTag(tag: String)(implicit outputStream: OutputStream): Unit = {
    for(c <- s"<$tag>") {
      outputStream.write(c)
    }
  }

  def writeEndTag(tag: String)(implicit outputStream: OutputStream): Unit = {
    for(c <- s"</$tag>") {
      outputStream.write(c)
    }
  }

  def placeOnStartTag(tag: String)(implicit streamReader: XMLStreamReader): Unit = {
    while((streamReader.getEventType != XMLStreamConstants.START_ELEMENT || streamReader.getLocalName != tag) && streamReader.hasNext) {
      streamReader.next()
    }
  }

  def placeOnNextTagAfterStartTag(tag: String)(implicit streamReader: XMLStreamReader): Unit = {
    placeOnStartTag(tag)
    if(streamReader.hasNext) {
      placeOnNextTag()
    }
  }

  // Place stream reader on next start or end tag
  def placeOnNextTag()(implicit streamReader: XMLStreamReader): Unit = {
    streamReader.next()
    while((streamReader.getEventType != XMLStreamConstants.START_ELEMENT && streamReader.getEventType != XMLStreamConstants.END_ELEMENT) && streamReader.hasNext) {
      streamReader.next()
    }
  }

  /** Reads multiple objects from the XML stream that follow each other and have the same tag if defined.
    * If no tag is defined it will try to read all elements that directly follow each other. */
  def readObjects[U](expectedTag: Option[String] = None)
                    (implicit streamReader: XMLStreamReader, readContext: ReadContext, xmlFormat: XmlFormat[U]): Seq[U] = {
    val entityBuffer = ArrayBuffer[U]()
    while(streamReader.getEventType == XMLStreamConstants.START_ELEMENT && expectedTag.forall(_ == streamReader.getLocalName)) {
      val entity = readNextObject[U]
      entityBuffer.append(entity)
      placeOnNextTag()
    }
    entityBuffer.toSeq
  }

  /** Reads multiple objects from the XML stream that follow each other and have the same tag if defined.
    * If no tag is defined it will try to read all elements that directly follow each other. */
  def convertObjects[U](expectedTag: Option[String] = None, convert: (Node) => U)
                    (implicit streamReader: XMLStreamReader, transformer: Transformer, readContext: ReadContext): Seq[U] = {
    val entityBuffer = ArrayBuffer[U]()
    while(streamReader.getEventType == XMLStreamConstants.START_ELEMENT && expectedTag.forall(_ == streamReader.getLocalName)) {
      val node = readCurrentElementAsNode
      entityBuffer.append(convert(node))
      placeOnNextTag()
    }
    entityBuffer.toSeq
  }

  /** Read a single object from the XML stream, i.e. it turns the current XML element into a DOM node and then converts this DOM node into a Scala object. */
  def readNextObject[U](implicit streamReader: XMLStreamReader, readContext: ReadContext, xmlFormat: XmlFormat[U]): U = {
    val node = readCurrentElementAsNode
    XmlSerialization.fromXml[U](node)
  }

  /** Reads the current element of the stream as an XML Node.
    * The stream will be positioned directly after END element of that element afterwards. */
  private def readCurrentElementAsNode(implicit reader: XMLStreamReader): Node = {
    assert(reader.isStartElement, "Trying to read an element from XML, but XML stream is positioned at event type: " + reader.getEventType)

    // Remember label and attributes
    val label = reader.getLocalName
    val attributes = readAttributes()

    // Collect child nodes
    val children = new ArrayBuffer[Node]()
    reader.next()
    while (!reader.isEndElement) {
      if (reader.isStartElement) {
        children.append(readCurrentElementAsNode)
      } else if (reader.isCharacters) {
        children.append(Text(reader.getText))
        reader.next()
      } else {
        reader.next()
      }
    }

    // Move to the element after the end element.
    reader.next()

    Elem(null, label, attributes, TopScope, true, children: _*)
  }

  // Reads attributes of the current element
  private def readAttributes()(implicit streamReader: XMLStreamReader): MetaData = {
    var metaData: MetaData = scala.xml.Null
    for (i <- 0 until streamReader.getAttributeCount) {
      metaData = metaData.copy(new UnprefixedAttribute(streamReader.getAttributeLocalName(i), Text(streamReader.getAttributeValue(i)), scala.xml.Null))
    }
    metaData
  }
}

object StreamXml {
  def write[T](value: T, outputStream: OutputStream)(implicit streamXmlFormat: StreamXmlFormat[T]): Unit = {
    streamXmlFormat.write(value, outputStream)
  }

  def read[T](inputStream: InputStream)(implicit streamXmlFormat: StreamXmlFormat[T], readContext: ReadContext): T = {
    streamXmlFormat.read(inputStream)
  }
}
