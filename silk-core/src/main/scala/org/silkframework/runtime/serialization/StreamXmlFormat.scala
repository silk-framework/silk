package org.silkframework.runtime.serialization

import java.io.{InputStream, OutputStream}
import javax.xml.stream.util.StreamReaderDelegate
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}

import com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX

import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter

/**
  * Serialization format that serializes Scala objects to XML and back using streaming.
  */
trait StreamXmlFormat[T] {
  /** Read a value from the input stream */
  def read(streamReader: XMLStreamReader)(implicit readContext: ReadContext): T

  /** Write value as XML to an output stream */
  def write(value: T, outputStream: OutputStream): Unit

  def read(inputStream: InputStream)(implicit readContext: ReadContext): T = {
    val xmlInputFactory = XMLInputFactory.newInstance()
    val xmlStreamReader = new StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(inputStream)) {
      override def getVersion: String = "1.0" // This is needed because else this will result in a NullPointerException
    }
    xmlStreamReader.nextTag()
    read(xmlStreamReader)
  }

  /** Convert DOM Node to Scala XML Node */
  def asXml(dom: org.w3c.dom.Node): Node = {
    val dom2sax = new DOM2SAX(dom)
    val adapter = new NoBindingFactoryAdapter
    dom2sax.setContentHandler(adapter)
    dom2sax.parse()
    adapter.rootElem
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