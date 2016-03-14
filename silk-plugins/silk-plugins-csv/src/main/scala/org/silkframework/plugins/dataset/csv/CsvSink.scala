package org.silkframework.plugins.dataset.csv

import java.io._

import org.silkframework.dataset.DataSink
import org.silkframework.runtime.resource.{WritableResource, FileResource, Resource}

class CsvSink(resource: WritableResource, settings: CsvSettings) extends DataSink {

  private val javaFile = resource match {
    case f: FileResource => Some(f.file)
    case _ => None
  }

  @volatile
  private var out: Writer = null

  def write(s: String): Unit = {
    out.write(s)
  }

  def open(properties: Seq[String] = Seq.empty) {
    javaFile match {
      case Some(file) =>
        // Use a buffered writer that directly writes to the file
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
      case None =>
        out = new StringWriter()
    }
    //Write header
    if(properties.nonEmpty)
      out.write(properties.mkString(settings.separator.toString) + "\n")
  }

  def close() {
    if (out != null) {
      out.close()
      // If we are using a string writer, we still need to write the data to the resource
      out match {
        case stringWriter: StringWriter =>
          resource.write(stringWriter.toString)
        case _ =>
      }
      out = null
    }
  }
}
