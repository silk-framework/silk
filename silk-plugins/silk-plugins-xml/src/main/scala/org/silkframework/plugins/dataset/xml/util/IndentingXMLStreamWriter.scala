package org.silkframework.plugins.dataset.xml.util

import org.silkframework.plugins.dataset.xml.util.IndentingXMLStreamWriter.State
import org.silkframework.plugins.dataset.xml.util.IndentingXMLStreamWriter.State.State

import java.util
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.{XMLStreamException, XMLStreamWriter}

/**
  * XML Stream writer to do simple pretty printing.
  * Based on the corresponding class in the Java SDK.
  */
class IndentingXMLStreamWriter(writer: XMLStreamWriter, indentStep: String = "  ") extends XMLStreamWriter{

  private var state: State = State.SEEN_NOTHING
  private val stateStack: util.Stack[State] = new util.Stack[State]
  private var depth: Int = 0

  @throws[XMLStreamException]
  private def onStartElement(): Unit = {
    stateStack.push(State.SEEN_ELEMENT)
    state = State.SEEN_NOTHING
    if (depth > 0) writer.writeCharacters("\n")
    doIndent()
    depth += 1
  }

  @throws[XMLStreamException]
  private def onEndElement(): Unit = {
    depth -= 1
    if (state == State.SEEN_ELEMENT) {
      writer.writeCharacters("\n")
      doIndent()
    }
    state = stateStack.pop
  }

  @throws[XMLStreamException]
  private def onEmptyElement(): Unit = {
    state = State.SEEN_ELEMENT
    if (depth > 0) writer.writeCharacters("\n")
    doIndent()
  }

  /**
    * Print indentation for the current level.
    *
    * @exception org.xml.sax.SAXException If there is an error
    *            writing the indentation characters, or if a filter
    *            further down the chain raises an exception.
    */
  @throws[XMLStreamException]
  private def doIndent(): Unit = {
    if (depth > 0) for (i <- 0 until depth) {
      writer.writeCharacters(indentStep)
    }
  }

  @throws[XMLStreamException]
  override def writeStartDocument(): Unit = {
    writer.writeStartDocument()
    writer.writeCharacters("\n")
  }

  @throws[XMLStreamException]
  override def writeStartDocument(version: String): Unit = {
    writer.writeStartDocument(version)
    writer.writeCharacters("\n")
  }

  @throws[XMLStreamException]
  override def writeStartDocument(encoding: String, version: String): Unit = {
    writer.writeStartDocument(encoding, version)
    writer.writeCharacters("\n")
  }

  @throws[XMLStreamException]
  override def writeStartElement(localName: String): Unit = {
    onStartElement()
    writer.writeStartElement(localName)
  }

  @throws[XMLStreamException]
  override def writeStartElement(namespaceURI: String, localName: String): Unit = {
    onStartElement()
    writer.writeStartElement(namespaceURI, localName)
  }

  @throws[XMLStreamException]
  override def writeStartElement(prefix: String, localName: String, namespaceURI: String): Unit = {
    onStartElement()
    writer.writeStartElement(prefix, localName, namespaceURI)
  }

  @throws[XMLStreamException]
  override def writeEmptyElement(namespaceURI: String, localName: String): Unit = {
    onEmptyElement()
    writer.writeEmptyElement(namespaceURI, localName)
  }

  @throws[XMLStreamException]
  override def writeEmptyElement(prefix: String, localName: String, namespaceURI: String): Unit = {
    onEmptyElement()
    writer.writeEmptyElement(prefix, localName, namespaceURI)
  }

  @throws[XMLStreamException]
  override def writeEmptyElement(localName: String): Unit = {
    onEmptyElement()
    writer.writeEmptyElement(localName)
  }

  @throws[XMLStreamException]
  override def writeEndElement(): Unit = {
    onEndElement()
    writer.writeEndElement()
  }

  @throws[XMLStreamException]
  override def writeCharacters(text: String): Unit = {
    state = State.SEEN_DATA
    writer.writeCharacters(text)
  }

  @throws[XMLStreamException]
  override def writeCharacters(text: Array[Char], start: Int, len: Int): Unit = {
    state = State.SEEN_DATA
    writer.writeCharacters(text, start, len)
  }

  @throws[XMLStreamException]
  override def writeCData(data: String): Unit = {
    state = State.SEEN_DATA
    writer.writeCData(data)
  }

  override def close(): Unit = {
    writer.close()
  }

  override def flush(): Unit = {
    writer.flush()
  }

  override def getNamespaceContext: NamespaceContext = {
    writer.getNamespaceContext
  }

  override def getPrefix(uri: String): String = {
    writer.getPrefix(uri)
  }

  override def getProperty(name: String): AnyRef = {
    writer.getProperty(name)
  }

  override def setDefaultNamespace(uri: String): Unit = {
    writer.setDefaultNamespace(uri)
  }

  override def setNamespaceContext(context: NamespaceContext): Unit = {
    writer.setNamespaceContext(context)
  }

  override def setPrefix(prefix: String, uri: String): Unit = {
    writer.setPrefix(prefix, uri)
  }

  override def writeAttribute(localName: String, value: String): Unit = {
    writer.writeAttribute(localName, value)
  }

  override def writeAttribute(namespaceURI: String, localName: String, value: String): Unit = {
    writer.writeAttribute(namespaceURI, localName, value)
  }

  override def writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String): Unit = {
    writer.writeAttribute(prefix, namespaceURI, localName, value)
  }

  override def writeComment(data: String): Unit = {
    writer.writeComment(data)
  }

  override def writeDefaultNamespace(namespaceURI: String): Unit = {
    writer.writeDefaultNamespace(namespaceURI)
  }

  override def writeDTD(dtd: String): Unit = {
    writer.writeDTD(dtd)
  }

  override def writeEndDocument(): Unit = {
    writer.writeEndDocument()
  }

  override def writeEntityRef(name: String): Unit = {
    writer.writeEntityRef(name)
  }

  override def writeNamespace(prefix: String, namespaceURI: String): Unit = {
    writer.writeNamespace(prefix, namespaceURI)
  }

  override def writeProcessingInstruction(target: String): Unit = {
    writer.writeProcessingInstruction(target)
  }

  override def writeProcessingInstruction(target: String, data: String): Unit = {
    writer.writeProcessingInstruction(target, data)
  }
}

object IndentingXMLStreamWriter {

  object State extends Enumeration {
    type State = Value
    val SEEN_NOTHING, SEEN_ELEMENT, SEEN_DATA = Value
  }

}