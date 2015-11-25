package org.silkframework.plugins.dataset.rdf.formatters

import java.io._
import org.silkframework.dataset.DataSink
import org.silkframework.entity.Link
import org.silkframework.runtime.resource.{WritableResource, Resource, FileResource}

class FormattedDataSink(resource: WritableResource, formatter: Formatter) extends DataSink {

  private var properties = Seq[String]()

  // We optimize cases in which the resource is a file resource
  private val javaFile = resource match {
    case f: FileResource => Some(f.file)
    case _ => None
  }

  private var writer: Writer = null

  override def open(properties: Seq[String]) {
    this.properties = properties
  }

  override def init(): Unit = {
    // If we got a java file, we write directly to it, otherwise we write to a temporary string
    writer = javaFile match {
      case Some(file) =>
        file.getParentFile.mkdirs()
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
      case None => new StringWriter()
    }
    //Write header
    writer.write(formatter.header)
  }

  override def writeLink(link: Link, predicateUri: String) {
    writer.write(formatter.format(link, predicateUri))
  }

  override def writeEntity(subject: String, values: Seq[Set[String]]) {
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      writer.write(formatter.formatLiteralStatement(subject, property, value))
    }
  }

  override def close() {
    if (writer != null) {
      writer.write(formatter.footer)
      writer.close()
      // In case we used a string writer, we still need to write the generated string.
      writer match {
        case stringWriter: StringWriter => resource.write(stringWriter.toString)
        case _ =>
      }
      writer = null
    }
  }
}