package de.fuberlin.wiwiss.silk.util

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import javax.xml.transform.stream.StreamSource
import xml.{Node, TopScope, Elem}
import java.io._
import org.xml.sax.{SAXParseException, ErrorHandler, InputSource}
import xml.parsing.NoBindingFactoryAdapter

/**
 * Parses an XML input source and validates it against the schema.
 */
class ValidatingXMLReader[T](deserializer : Node => T, schemaPath : String)
{
  def apply(xml : Node) : T =
  {
    //Validate
    new XmlReader().read(new InputSource(new StringReader(xml.toString)), schemaPath)

    deserializer(xml)
  }

  def apply(stream : InputStream) : T =
  {
    val node = new XmlReader().read(new InputSource(stream), schemaPath)

    deserializer(node)
  }

  def apply(reader : Reader) : T =
  {
    val node = new XmlReader().read(new InputSource(reader), schemaPath)

    deserializer(node)
  }

  def apply(file : File) : T =
  {
    val inputStream = new FileInputStream(file)
    try
    {
      apply(inputStream)
    }
    finally
    {
      inputStream.close()
    }
  }

  private class XmlReader extends NoBindingFactoryAdapter
  {
    def read(inputSource : InputSource, schemaPath : String) : Elem =
    {
      //Load XML Schema
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schemaStream = getClass.getClassLoader.getResourceAsStream(schemaPath)
      if(schemaStream == null) throw new ValidationException("XML Schema for Link Specification not found")
      val schema = schemaFactory.newSchema(new StreamSource(schemaStream))

      //Create parser
      val parserFactory = SAXParserFactory.newInstance()
      parserFactory.setNamespaceAware(true)
      parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      val parser = parserFactory.newSAXParser()

      //Set Error handler
      var errors = List[String]()
      val xr = parser.getXMLReader
      val vh = schema.newValidatorHandler()
      vh.setErrorHandler(new ErrorHandler
      {
        def warning(ex: SAXParseException) { }

        def error(ex: SAXParseException) { errors ::= formatError("Error", ex) }

        def fatalError(ex: SAXParseException) { errors ::= formatError("Fatal Error", ex) }
      })
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      //Parse XML
      scopeStack.push(TopScope)
      xr.parse(inputSource)
      scopeStack.pop

      //Return result
      if(errors.isEmpty)
      {
        val xml = rootElem.asInstanceOf[Elem]
        checkUniqueIdentifiers(xml)
        xml
      }
      else
      {
        throw new ValidationException(errors.reverse.mkString("\n"))
      }
    }

    /**
     * Checks if the document contains any duplicated identifiers.
     */
    private def checkUniqueIdentifiers(xml: Elem) {
      val ids = (xml \\ "@id").map(_.text)
      if(ids.distinct.size < ids.size) {
        val duplicatedIds = ids diff ids.distinct
        val errors = duplicatedIds.map("Duplicated identifier: '" + _ + "'")
        throw new ValidationException(errors)
      }
    }

    def formatError(errorType: String, ex: SAXParseException) =
    {
      //The current tag
      val tag = this.curTag
      //The id of the current tag
      val id = attribStack.head.find(_.key == "id").map(_.value.mkString)
      //The error message without prefixes like "cvc-complex-type.2.4.b:"
      val msg = ex.getMessage.split(':').tail.mkString.trim

      id match
      {
        case Some(i) => errorType + " in " + tag + " with id '" + i + "': " + msg
        case None => errorType + " in " + tag + ":" + msg
      }
    }
  }
}