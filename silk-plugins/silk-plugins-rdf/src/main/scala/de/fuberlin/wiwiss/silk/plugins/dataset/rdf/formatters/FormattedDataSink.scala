package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters

import java.io.{FileOutputStream, OutputStreamWriter, BufferedWriter, Writer}
import de.fuberlin.wiwiss.silk.dataset.{Formatter, DataSink}
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.resource.{Resource, FileResource}

class FormattedDataSink(resource: Resource, formatter: Formatter) extends DataSink {

  private var properties = Seq[String]()

  private val javaFile = resource match {
    case f: FileResource => f.file
    case _ => throw new IllegalArgumentException("Can only write to files, but got a resource of type " + resource.getClass)
  }

  private var out: Writer = null

  override def open(properties: Seq[String]) {
    this.properties = properties
    //Create buffered writer
    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(javaFile), "UTF-8"))
    //Write header
    out.write(formatter.header)
  }

  override def writeLink(link: Link, predicateUri: String) {
    out.write(formatter.format(link, predicateUri))
  }

  override def writeEntity(subject: String, values: Seq[Set[String]]) {
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      out.write(formatter.formatLiteralStatement(subject, property, value))
    }
  }

  override def close() {
    if (out != null) {
      out.write(formatter.footer)
      out.close()
      out = null
    }
  }
}