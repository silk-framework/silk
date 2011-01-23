package de.fuberlin.wiwiss.silk.util

import xml.parsing.NoBindingFactoryAdapter
import org.xml.sax.{SAXException, InputSource}
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import javax.xml.transform.stream.StreamSource
import xml.{Node, TopScope, Elem}
import java.io.{FileInputStream, File, Reader, InputStream}

/**
 * Parses an XML input source and validates it against the schema.
 */
class ValidatingXMLReader[T](deserializer : Node => T, schemaPath : String) extends NoBindingFactoryAdapter
{
  def apply(stream : InputStream) : T =
  {
    val node = read(new InputSource(stream), schemaPath)

    deserializer(node)
  }

  def apply(reader : Reader) : T =
  {
    val node = read(new InputSource(reader), schemaPath)

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

  private def read(inputSource : InputSource, schemaPath : String) : Elem =
  {
    try
    {
      //Load XML Schema
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schemaStream = getClass().getClassLoader().getResourceAsStream(schemaPath)
      if(schemaStream == null) throw new ValidationException("XML Schema for Link Specification not found")
      val schema = schemaFactory.newSchema(new StreamSource(schemaStream))

      //Create parser
      val parserFactory = SAXParserFactory.newInstance()
      parserFactory.setNamespaceAware(true)
      parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      val parser = parserFactory.newSAXParser()

      val xr = parser.getXMLReader()
      val vh = schema.newValidatorHandler()
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      //Parse XML
      scopeStack.push(TopScope)
      xr.parse(inputSource)
      scopeStack.pop

      rootElem.asInstanceOf[Elem]
    }
    catch
    {
      case ex : SAXException => throw new ValidationException("Invalid XML. Details: " + ex.getMessage, ex)
    }
  }
}