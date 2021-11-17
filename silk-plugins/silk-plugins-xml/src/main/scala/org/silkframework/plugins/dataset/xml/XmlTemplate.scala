package org.silkframework.plugins.dataset.xml

import org.silkframework.runtime.validation.ValidationException
import org.w3c.dom.{Document, Node, ProcessingInstruction}

import java.io.StringReader
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stax.StAXResult
import scala.xml.InputSource

case class XmlTemplate(doc: Document, rootElementName: String, isSingleInstruction: Boolean) {

  def writePrefix(writer: XMLStreamWriter): Unit = {
    if(isSingleInstruction) {
      writer.writeStartDocument()
    } else {
      writeTemplate(writer, writeSuffix = false)
    }

  }

  def writeSuffix(writer: XMLStreamWriter): Unit = {
    if(isSingleInstruction) {
      writer.writeEndDocument()
    } else {
      writeTemplate(writer, writeSuffix = true)
    }
  }

  private def writeTemplate(writer: XMLStreamWriter, writeSuffix: Boolean): Unit = {
    val source = new DOMSource(doc)
    val result = new StAXResult(new CustomXMLStreamWriter(writer, writeSuffix))

    val tFactory = TransformerFactory.newInstance
    val transformer = tFactory.newTransformer()
    transformer.transform(source, result)
  }

}

object XmlTemplate {

  def parse(templateStr: String): XmlTemplate = {
    val isSingleInstruction = templateStr.matches("<\\?[^\\?]+\\?>")
    val builder = DocumentBuilderFactory.newInstance.newDocumentBuilder
    if(isSingleInstruction) {
      val rootElementName = templateStr.substring(2, templateStr.length - 2)
      XmlTemplate(builder.newDocument(), rootElementName, isSingleInstruction)
    } else {
      val doc = builder.parse(new InputSource(new StringReader(templateStr)))
      val rootElementName = findEntityTemplate(doc).getTarget
      XmlTemplate(doc, rootElementName, isSingleInstruction)
    }
  }

  private def findEntityTemplate(node: Node): ProcessingInstruction = {
    findEntityTemplateRecursive(node) match {
      case Some(pi) =>
        pi
      case None =>
        throw new ValidationException("Could not find template entity of the form <?Entity?>")
    }
  }

  private def findEntityTemplateRecursive(node: Node): Option[ProcessingInstruction] = {
    if(node.isInstanceOf[ProcessingInstruction]) {
      Some(node.asInstanceOf[ProcessingInstruction])
    } else if(node.hasChildNodes) {
      val children = node.getChildNodes
      for(i <- 0 until children.getLength) {
        findEntityTemplateRecursive(children.item(i)) match {
          case pi @ Some(_) =>
            return pi
          case None => // Do nothing
        }
      }
      None
    } else {
      None
    }
  }

}

private class CustomXMLStreamWriter(writer: XMLStreamWriter, writeSuffix: Boolean) extends XMLStreamWriter {

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