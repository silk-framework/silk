package de.fuberlin.wiwiss.silk.util

import java.io._
import xml.{PrettyPrinter, NodeSeq}

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
object XMLUtils {
  implicit def toXMLUtils(xml: NodeSeq) = new XMLUtils(xml)
}

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
class XMLUtils(xml: NodeSeq) {
  def toFormattedString = {
    val stringWriter = new StringWriter()
    write(stringWriter)
    stringWriter.toString
  }

  def write(file: File) {
    val fileWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    try {
      write(fileWriter)
    }
    finally {
      fileWriter.close()
    }
  }

  def write(writer: Writer) {
    val printer = new PrettyPrinter(Int.MaxValue, 2)

    writer.write(printer.formatNodes(xml))
    writer.write("\n")
    writer.flush()
  }
}
