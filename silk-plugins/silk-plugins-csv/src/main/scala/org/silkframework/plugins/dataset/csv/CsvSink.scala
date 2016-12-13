package org.silkframework.plugins.dataset.csv

import java.io._

import org.silkframework.dataset.{DataSink, TypedProperty}
import org.silkframework.runtime.resource.{FileResource, Resource, WritableResource}

class CsvSink(resource: WritableResource, settings: CsvSettings) extends DataSink {

  private val javaFile = resource match {
    case f: FileResource => Some(f.file)
    case _ => None
  }

  @volatile
  private var out: Writer = _

  def write(s: String): Unit = {
    out.write(s)
  }

  def open(properties: Seq[TypedProperty] = Seq.empty) {
    javaFile match {
      case Some(file) =>
        // Use a buffered writer that directly writes to the file
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
      case None =>
        out = new StringWriter()
    }
    //Write header
    if(properties.nonEmpty) {
      out.write(properties.map(_.propertyUri).mkString(settings.separator.toString) + "\n")
    }
  }

  def close() {
    if (Option(out).isDefined) {
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
