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

import java.io._
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import ValidationException.ValidationError
import org.silkframework.util.Identifier
import org.xml.sax.{Attributes, ErrorHandler, InputSource, SAXParseException}

import scala.xml._
import scala.xml.parsing.NoBindingFactoryAdapter

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
  private class XmlReader extends NoBindingFactoryAdapter {
    private var currentErrors = List[String]()
    private var validationErrors = List[ValidationError]()

    def read(inputSource: InputSource, schemaPath: String): Elem = {
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
        def warning(ex: SAXParseException) {}

        def error(ex: SAXParseException) {
          addError(ex)
        }

        def fatalError(ex: SAXParseException) {
          addError(ex)
        }
      })
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      //Parse XML
      scopeStack.push(TopScope)
      xr.parse(inputSource)
      scopeStack.pop()

      //Add errors without an id
      for(error <- currentErrors) {
        validationErrors ::= ValidationError(error)
      }

      //Return result
      if (validationErrors.isEmpty) {
        val xml = rootElem.asInstanceOf[Elem]
        checkUniqueIdentifiers(xml)
        xml
      }
      else {
        throw new ValidationException(validationErrors.reverse)
      }
    }

    override def startElement(uri: String, _localName: String, qname: String, attributes: Attributes) {
      // Add all errors for this element before advancing
      for(idAttribute <- Option(attributes.getValue("id"))) {
        // Try to get identifier of this element
        val id =
          try {
            Some(Identifier(idAttribute))
          } catch {
            case ex: Exception =>
              println(_localName + ": " + ex.getMessage)
              validationErrors ::= ValidationError(ex.getMessage, None, Some(_localName))
              None
          }

        for(error <- currentErrors) {
          validationErrors ::= ValidationError(error, id, Some(_localName))
        }

        currentErrors = Nil
      }

      super.startElement(uri, _localName, qname, attributes)
    }

    /**
     * Checks if the document contains any duplicated identifiers.
     */
    private def checkUniqueIdentifiers(xml: Elem) {
      val elements = (xml \\ "Aggregate") ++ (xml \\ "Compare") ++ (xml \\ "TransformInput") ++ (xml \\ "Input")
      val ids = elements.map(_ \ "@id").map(_.text).filterNot(_.isEmpty)
      if (ids.distinct.size < ids.size) {
        val duplicatedIds = ids diff ids.distinct
        val errors = duplicatedIds.map(id => ValidationError("Duplicated identifier", Some(Identifier(id))))
        throw new ValidationException(errors)
      }
    }

    /**
     * Formats a XSD validation exception.
     */
    private def addError(ex: SAXParseException) = {
      //The error message without prefixes like "cvc-complex-type.2.4.b:"
      val error = ex.getMessage.split(':').tail.mkString.trim

      currentErrors ::= error
    }
  }

}

object ValidatingXMLReader {

  def validate(node: Node, schemaPath: String): Unit = {
    new ValidatingXMLReader(schemaPath)(node)
  }

}