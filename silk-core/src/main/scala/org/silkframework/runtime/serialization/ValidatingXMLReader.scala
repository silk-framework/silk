/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.runtime.serialization

import java.io.*
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import org.silkframework.runtime.validation.{ValidationError, ValidationException}
import org.silkframework.util.Identifier
import org.xml.sax.{Attributes, ContentHandler, ErrorHandler, InputSource, Locator, SAXParseException}

import scala.xml.*

/**
 * Parses an XML input source and validates it against the schema.
 */
class ValidatingXMLReader(schemaPath: String) {

  def apply(xml: Node): Unit = {
    new XmlReader().read(new InputSource(new StringReader(xml.toString)), schemaPath)
  }

  def apply(stream: InputStream): Unit = {
    new XmlReader().read(new InputSource(stream), schemaPath)
  }

  def apply(reader: Reader): Unit = {
    new XmlReader().read(new InputSource(reader), schemaPath)
  }

  def apply(file: File): Unit = {
    val inputStream = new FileInputStream(file)
    try {
      apply(inputStream)
    }
    finally {
      inputStream.close()
    }
  }

  /**
   * Reads an XML stream while validating it using a xsd schema file.
   */
  private class XmlReader extends ContentHandler {
    private var currentErrors = List[String]()
    private var validationErrors = List[ValidationError]()

    def read(inputSource: InputSource, schemaPath: String): Unit = {
      //Load XML Schema
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schemaStream = getClass.getClassLoader.getResourceAsStream(schemaPath)
      if (schemaStream == null) throw new ValidationException("XML Schema for Link Specification not found")
      val schema = schemaFactory.newSchema(new StreamSource(schemaStream))

      //Create parser
      val parserFactory = SAXParserFactory.newInstance()
      parserFactory.setNamespaceAware(true)
      parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      val parser = parserFactory.newSAXParser()

      //Set Error handler
      val xr = parser.getXMLReader
      val vh = schema.newValidatorHandler()
      vh.setErrorHandler(new ErrorHandler {
        def warning(ex: SAXParseException): Unit = {}

        def error(ex: SAXParseException): Unit = {
          addError(ex)
        }

        def fatalError(ex: SAXParseException): Unit = {
          addError(ex)
        }
      })
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      //Parse XML
      xr.parse(inputSource)

      //Add errors without an id
      for(error <- currentErrors) {
        validationErrors ::= ValidationError(error)
      }

      //Return result
      if (validationErrors.nonEmpty) {
        throw new ValidationException(validationErrors.reverse)
      }
    }

    override def startElement(uri: String, _localName: String, qname: String, attributes: Attributes): Unit = {
      // Add all errors for this element before advancing
      for(idAttribute <- Option(attributes.getValue("id"))) {
        // Try to get identifier of this element
        val id =
          try {
            Some(Identifier(idAttribute))
          } catch {
            case ex: Exception =>
              validationErrors ::= ValidationError(ex.getMessage, None, Some(_localName))
              None
          }

        for(error <- currentErrors) {
          validationErrors ::= ValidationError(error, id, Some(_localName))
        }

        currentErrors = Nil
      }
    }

    /**
     * Formats a XSD validation exception.
     */
    private def addError(ex: SAXParseException): Unit = {
      //The error message without prefixes like "cvc-complex-type.2.4.b:"
      val error = ex.getMessage.split(':').tail.mkString.trim

      currentErrors ::= error
    }

    override def setDocumentLocator(locator: Locator): Unit = {}

    override def startDocument(): Unit = {}

    override def endDocument(): Unit = {}

    override def startPrefixMapping(prefix: String, uri: String): Unit = {}

    override def endPrefixMapping(prefix: String): Unit = {}

    override def endElement(uri: String, localName: String, qName: String): Unit = {}

    override def characters(ch: Array[Char], start: Int, length: Int): Unit = {}

    override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {}

    override def processingInstruction(target: String, data: String): Unit = {}

    override def skippedEntity(name: String): Unit = {}
  }

}

object ValidatingXMLReader {

  def validate(node: Node, schemaPath: String): Unit = {
    new ValidatingXMLReader(schemaPath)(node)
  }

}