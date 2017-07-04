package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.{DataSink, TypedProperty}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

class CsvSink(resource: WritableResource, settings: CsvSettings) extends DataSink {

  @volatile
  private var writerOpt: Option[CsvWriter] = None

  def open(typeUri: Uri, properties: Seq[TypedProperty] = Seq.empty) {
    writerOpt = Some(new CsvWriter(resource, properties, settings))
  }

  def write(values: Seq[String]): Unit = {
    writerOpt match {
      case Some(writer) => writer.writeLine(values)
      case None => throw new IllegalStateException("Tried to write to CSV Sink that has not been opened.")
    }
  }

  def close() {
    for(writer <- writerOpt) {
      writer.close()
    }
    writerOpt = None
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = resource.delete()
}
