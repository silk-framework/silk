package org.silkframework.plugins.dataset.xml.util

import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLStreamWriter

/**
  * Delegating XMLStreamWriter that forwards all writes either before or after the processing instruction.
  *
  * @param writer      Output writer
  * @param writeSuffix If true, all writes after the processing instruction are forwarded.
  *                    If false, all writes before the processing instruction are forwarded.
  */
class PrefixSuffixXMLStreamWriter(writer: XMLStreamWriter, writeSuffix: Boolean) extends XMLStreamWriter {

  private var processingInstructionReached: Boolean = false

  private def forward: Boolean = writeSuffix && processingInstructionReached || !writeSuffix && !processingInstructionReached

  override def close(): Unit = if (forward) {
    writer.close()
  }

  override def flush(): Unit = writer.flush()

  override def getNamespaceContext: NamespaceContext = writer.getNamespaceContext

  override def getPrefix(uri: String): String = writer.getPrefix(uri)

  override def getProperty(name: String): AnyRef = writer.getProperty(name)

  override def setDefaultNamespace(uri: String): Unit = writer.setDefaultNamespace(uri)

  override def setNamespaceContext(context: NamespaceContext): Unit = writer.setNamespaceContext(context)

  override def setPrefix(prefix: String, uri: String): Unit = writer.setPrefix(prefix, uri)

  override def writeAttribute(localName: String, value: String): Unit = if (forward) {
    writer.writeAttribute(localName, value)
  }

  override def writeAttribute(namespaceURI: String, localName: String, value: String): Unit = if (forward) {
    writer.writeAttribute(namespaceURI, localName, value)
  }

  override def writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String): Unit = if (forward) {
    writer.writeAttribute(prefix, namespaceURI, localName, value)
  }

  override def writeCData(data: String): Unit = if (forward) {
    writer.writeCData(data)
  }

  override def writeCharacters(text: Array[Char], start: Int, len: Int): Unit = if (forward) {
    writer.writeCharacters(text, start, len)
  }

  override def writeCharacters(text: String): Unit = if (forward) {
    writer.writeCharacters(text)
  }

  override def writeComment(data: String): Unit = if (forward) {
    writer.writeComment(data)
  }

  override def writeDefaultNamespace(namespaceURI: String): Unit = if (forward) {
    writer.setDefaultNamespace(namespaceURI)
  }

  override def writeDTD(dtd: String): Unit = if (forward) {
    writer.writeDTD(dtd)
  }

  override def writeEmptyElement(localName: String): Unit = if (forward) {
    writer.writeEmptyElement(localName)
  }

  override def writeEmptyElement(namespaceURI: String, localName: String): Unit = if (forward) {
    writer.writeEmptyElement(namespaceURI, localName)
  }

  override def writeEmptyElement(prefix: String, localName: String, namespaceURI: String): Unit = if (forward) {
    writer.writeEmptyElement(prefix, localName, namespaceURI)
  }

  override def writeEndDocument(): Unit = if (forward) {
    writer.writeEndDocument()
  }

  override def writeEndElement(): Unit = if (forward) {
    writer.writeEndElement()
  }

  override def writeEntityRef(name: String): Unit = if (forward) {
    writer.writeEntityRef(name)
  }

  override def writeNamespace(prefix: String, namespaceURI: String): Unit = if (forward) {
    writer.writeNamespace(prefix, namespaceURI)
  }

  override def writeProcessingInstruction(target: String): Unit = {
    processingInstructionReached = true
  }

  override def writeProcessingInstruction(target: String, data: String): Unit = {
    processingInstructionReached = true
  }

  override def writeStartDocument(): Unit = if (forward) {
    writer.writeStartDocument()
  }

  override def writeStartDocument(version: String): Unit = if (forward) {
    writer.writeStartDocument(version)
  }

  override def writeStartDocument(encoding: String, version: String): Unit = if (forward) {
    writer.writeStartDocument(encoding, version)
  }

  override def writeStartElement(localName: String): Unit = if (forward) {
    writer.writeStartElement(localName)
  }

  override def writeStartElement(namespaceURI: String, localName: String): Unit = if (forward) {
    writer.writeStartElement(namespaceURI, localName)
  }

  override def writeStartElement(prefix: String, localName: String, namespaceURI: String): Unit = if (forward) {
    writer.writeStartElement(prefix, localName, namespaceURI)
  }
}
