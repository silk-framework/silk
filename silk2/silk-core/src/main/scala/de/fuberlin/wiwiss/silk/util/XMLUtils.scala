package de.fuberlin.wiwiss.silk.util

import java.io._
import xml.{TopScope, Elem, PrettyPrinter, NodeSeq}
import org.xml.sax.{SAXException, InputSource}
import javax.xml.parsers.SAXParserFactory
import javax.xml.XMLConstants
import javax.xml.validation.SchemaFactory
import javax.xml.transform.stream.StreamSource
import xml.parsing.NoBindingFactoryAdapter

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
object XMLUtils
{
  implicit def toXMLUtils(xml : NodeSeq) = new XMLUtils(xml)
}

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
class XMLUtils(xml : NodeSeq)
{
  def toFormattedString =
  {
    val stringWriter = new StringWriter()
    write(stringWriter)
    stringWriter.toString
  }

  def write(file : File)
  {
    val fileWriter= new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    try
    {
      write(fileWriter)
    }
    finally
    {
      fileWriter.close()
    }
  }

  def write(writer : Writer)
  {
    val printer = new PrettyPrinter(140, 2)

    writer.write(printer.formatNodes(xml))
    writer.write("\n")
    writer.flush()
  }
}
