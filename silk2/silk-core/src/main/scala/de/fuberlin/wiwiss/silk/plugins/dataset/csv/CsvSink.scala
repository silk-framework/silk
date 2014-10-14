package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import java.io.{FileOutputStream, OutputStreamWriter, BufferedWriter, Writer}

import de.fuberlin.wiwiss.silk.dataset.DataSink
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.resource.{FileResource, Resource}

class CsvSink(file: Resource, properties: String, separator: String = ",", arraySeparator: String = "", prefix: String = "", uri: String = "", regexFilter: String = "") extends DataSink {

  private val javaFile = file match {
    case f: FileResource => f.file
    case _ => throw new IllegalArgumentException("Can only write to files, but got a resource of type " + file.getClass)
  }

  private var out: Writer = null

  override def open() {
    //Create buffered writer
    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(javaFile), "UTF-8"))
    //Write header
    if(!properties.isEmpty)
      out.write(properties)
  }

  override def write(link: Link, predicateUri: String) {
    out.write(link.source + separator + link.target)
  }

  override def writeLiteralStatement(subject: String, predicate: String, value: String) {
    //TODO
    //out.write(formatter.formatLiteralStatement(subject, predicate, value))
  }

  override def close() {
    if (out != null) {
      out.close()
      out = null
    }
  }
}
