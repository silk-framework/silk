package org.silkframework.runtime.serialization

import java.io.{InputStream, OutputStream}
import javax.xml.stream.util.StreamReaderDelegate
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}
import javax.xml.transform.{Transformer, TransformerFactory}
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.stax.StAXSource

import com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter

/**
  * Serialization format that serializes Scala objects to XML and back using streaming.
  */
abstract class StreamXmlFormat[T: ClassTag] {

  /** Read a value from the input stream */
  def read(implicit streamReader: XMLStreamReader, readContext: ReadContext): T

  /** Write value as XML to an output stream */
  def write(value: T, outputStream: OutputStream): Unit

  def read(inputStream: InputStream)(implicit readContext: ReadContext): T = {
    val xmlInputFactory = XMLInputFactory.newInstance()
    implicit val xmlStreamReader = new StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(inputStream)) {
      override def getVersion: String = "1.0" // This is needed because else this will result in a NullPointerException
    }
    xmlStreamReader.nextTag()
    read
  }

  /** Convert DOM Node to Scala XML Node */
  def asXml(dom: org.w3c.dom.Node): Node = {
    val dom2sax = new DOM2SAX(dom)
    val adapter = new NoBindingFactoryAdapter
    dom2sax.setContentHandler(adapter)
    dom2sax.parse()
    adapter.rootElem
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
      streamReader.nextTag()
    }
  }

  def placeOnNextTagAfterStartTag(tag: String)(implicit streamReader: XMLStreamReader): Unit = {
    placeOnStartTag(tag)
    if(streamReader.hasNext) {
      streamReader.nextTag()
    }
  }

  /** Reads multiple objects from the XML stream that follow each other and have the same tag if defined.
    * If no tag is defined it will try to read all elements that directly follow each other. */
  def readObjects[U](expectedTag: Option[String] = None)
                    (implicit streamReader: XMLStreamReader, transformer: Transformer, readContext: ReadContext, xmlFormat: XmlFormat[U]): Seq[U] = {
    val entityBuffer = ArrayBuffer[U]()
    while(streamReader.getEventType == XMLStreamConstants.START_ELEMENT && expectedTag.forall(_ == streamReader.getLocalName)) {
      val entity = readNextObject[U]
      entityBuffer.append(entity)
      streamReader.nextTag()
    }
    entityBuffer
  }

  /** Reads multiple objects from the XML stream that follow each other and have the same tag if defined.
    * If no tag is defined it will try to read all elements that directly follow each other. */
  def convertObjects[U](expectedTag: Option[String] = None, convert: (Node) => U)
                    (implicit streamReader: XMLStreamReader, transformer: Transformer, readContext: ReadContext): Seq[U] = {
    val entityBuffer = ArrayBuffer[U]()
    while(streamReader.getEventType == XMLStreamConstants.START_ELEMENT && expectedTag.forall(_ == streamReader.getLocalName)) {
      val node = readCurrentElementAsNode
      entityBuffer.append(convert(node))
      streamReader.nextTag()
    }
    entityBuffer
  }

  /** Read a single object from the XML stream, i.e. it turns the current XML element into a DOM node and then converts this DOM node into a Scala object. */
  def readNextObject[U](implicit streamReader: XMLStreamReader, transformer: Transformer, readContext: ReadContext, xmlFormat: XmlFormat[U]): U = {
    val node = readCurrentElementAsNode
    XmlSerialization.fromXml[U](node)
  }

  def readCurrentElementAsNode(implicit streamReader: XMLStreamReader, transformer: Transformer): Node = {
    val result = new DOMResult()
    transformer.transform(new StAXSource(streamReader), result)
    asXml(result.getNode)
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