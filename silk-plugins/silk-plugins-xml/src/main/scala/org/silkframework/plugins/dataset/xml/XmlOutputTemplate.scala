package org.silkframework.plugins.dataset.xml

import net.sf.saxon.jaxp.SaxonTransformerFactory
import org.silkframework.plugins.dataset.xml.util.PrefixSuffixXMLStreamWriter
import org.silkframework.runtime.validation.ValidationException
import org.w3c.dom.{Document, Node, ProcessingInstruction}
import org.xml.sax.InputSource

import java.io.{ByteArrayInputStream, StringReader}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stax.StAXResult
import scala.util.{Failure, Success, Try}
import scala.xml._

/**
  * An XML output template.
  *
  * @param doc The XML tree of the template. Will be ignored if isRootTemplate is true.
  * @param rootElementName The name of the element to be generated.
  * @param isRootTemplate True, if this is a template of the form <?MyEntity?>, i.e., consists of a single processing instruction.
  */
case class XmlOutputTemplate(doc: Document, rootElementName: String, isRootTemplate: Boolean) {
  lazy val transformerFactory: TransformerFactory = {
    // Not all transformers support StAXResult objects as target, manually pick transformer
    new SaxonTransformerFactory()
  }

  def writePrefix(writer: XMLStreamWriter): Unit = {
    if(isRootTemplate) {
      writer.writeStartDocument()
    } else {
      writeTemplate(writer, writeSuffix = false)
    }

  }

  def writeSuffix(writer: XMLStreamWriter): Unit = {
    if(isRootTemplate) {
      writer.writeEndDocument()
    } else {
      writeTemplate(writer, writeSuffix = true)
    }
  }

  private def writeTemplate(writer: XMLStreamWriter, writeSuffix: Boolean): Unit = {
    val source = new DOMSource(doc)
    val result = new StAXResult(new PrefixSuffixXMLStreamWriter(writer, writeSuffix))
    val transformer = transformerFactory.newTransformer()
    transformer.transform(source, result)
  }

}

object XmlOutputTemplate {

  /**
    * Parses and validates an XML template.
    *
    * @throws ValidationException If the template is not valid.
    */
  def parse(templateStr: String): XmlOutputTemplate = {
    if(templateStr.matches("<\\?[^\\?]+\\?>")) {
      parseRootTemplate(templateStr)
    } else {
      parseFullXmlTemplate(templateStr)
    }
  }

  /**
    * Parses template of the form <?MyEntity?>, i.e., consists of a single processing instruction.
    */
  private def parseRootTemplate(templateStr: String) = {
    Try(XML.loadString(s"<Root>$templateStr</Root>")) match {
      case Success(_) =>
        val rootElementName = templateStr.substring(2, templateStr.length - 2)
        XmlOutputTemplate(DocumentBuilderFactory.newInstance.newDocumentBuilder.newDocument(), rootElementName, isRootTemplate = true)
      case Failure(ex: SAXParseException) =>
        throw new NoValidXmlException(ex)
      case Failure(ex) =>
        throw new InvalidXmlOutputTemplateException("Output template must be valid XML containing a single processing instruction or a single processing " +
          "instruction of the form <?Entity?>!", Some(ex))
    }
  }

  /**
    * Parses full XML templates, e.g., <Root><?MyEntity?></Root>
    */
  private def parseFullXmlTemplate(templateStr: String) = {
    val factory = DocumentBuilderFactory.newInstance
    factory.setValidating(false)
    val builder = factory.newDocumentBuilder
    // Don't load external DTDs etc.
    builder.setEntityResolver((publicId: String, systemId: String) => new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes())))
    val doc = try {
      builder.parse(new InputSource(new StringReader(templateStr)))
    } catch {
      case ex: SAXParseException =>
        throw new XmlOutputTemplate.NoValidXmlException(ex)
    }
    val rootElementName = findEntityTemplate(doc).getTarget
    XmlOutputTemplate(doc, rootElementName, isRootTemplate = false)
  }

  private def findEntityTemplate(node: Node): ProcessingInstruction = {
    findEntityTemplates(node) match {
      case Seq(pi) =>
        pi
      case Seq() =>
        throw new NoProcessingInstructionException()
      case _ =>
        throw new MultipleProcessingInstructionsException()
    }
  }

  private def findEntityTemplates(node: Node): Seq[ProcessingInstruction] = {
    node match {
      case instruction: ProcessingInstruction =>
        Seq(instruction)
      case _ if node.hasChildNodes =>
        val children = node.getChildNodes
        for  {
          i <- 0 until children.getLength
          instruction <- findEntityTemplates(children.item(i))
        } yield instruction
     case _ =>
       Seq.empty
    }
  }

  /**
    * Thrown if the XML output template is invalid.
    */
  class InvalidXmlOutputTemplateException(message: String, cause: Option[Throwable] = None) extends ValidationException(message, cause.orNull)

  /**
    * Thrown if the template is no valid XML.
    */
  class NoValidXmlException(ex: SAXParseException) extends InvalidXmlOutputTemplateException(
    "Output template could not be processed as valid XML. Error in line " + ex.getLineNumber + " column " + ex.getColumnNumber, Some(ex))

  /**
    * Thrown if the template does not contain exactly one processing instruction.
    */
  class NoProcessingInstructionException() extends InvalidXmlOutputTemplateException(
    "Output template must contain a processing instruction of the form <?Entity?> to specify where the entities should be inserted.")

  /**
    * Thrown if the template does not contain exactly one processing instruction.
    */
  class MultipleProcessingInstructionsException() extends InvalidXmlOutputTemplateException(
    "Output template must contain exactly one processing instruction of the form <?Entity?> to specify where the entities should be inserted."  +
    " Multiple processing instructions found")

}

