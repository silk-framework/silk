package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import java.io.{BufferedWriter, FileOutputStream, OutputStreamWriter, Writer}

import de.fuberlin.wiwiss.silk.dataset.DataSink
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.resource.{FileResource, Resource}

class CsvSink(file: Resource, settings: CsvSettings) extends DataSink {

  private val javaFile = file match {
    case f: FileResource => f.file
    case _ => throw new IllegalArgumentException("Can only write to files, but got a resource of type " + file.getClass)
  }

  @volatile
  private var out: Writer = null

  override def open(properties: Seq[String] = Seq.empty) {
    //Create buffered writer
    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(javaFile), "UTF-8"))
    //Write header
    if(properties.nonEmpty)
      out.write(properties.mkString(settings.separator.toString) + "\n")
  }

  override def writeLink(link: Link, predicateUri: String) {
    out.write(link.source + settings.separator + link.target + "\n")
  }

  override def writeEntity(subject: String, values: Seq[Set[String]]) {
    out.write(values.map(_.mkString(settings.arraySeparator)).mkString(settings.separator.toString) + "\n")
  }

  override def close() {
    if (out != null) {
      out.close()
      out = null
    }
  }
}
