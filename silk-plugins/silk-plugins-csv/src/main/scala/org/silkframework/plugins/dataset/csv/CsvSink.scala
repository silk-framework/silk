package org.silkframework.plugins.dataset.csv

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSink, DirtyTrackingFileDataSink, TypedProperty}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

import java.io.{File, IOException}
import java.util.logging.Logger

class CsvSink(val resource: WritableResource, settings: CsvSettings) extends DataSink with DirtyTrackingFileDataSink {
  private val log: Logger = Logger.getLogger(getClass.getName)

  @volatile
  private var writerOpt: Option[CsvWriter] = None

  def openTable(typeUri: Uri, properties: Seq[TypedProperty] = Seq.empty, singleEntity: Boolean = false)
               (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    writerOpt = Some(new CsvWriter(resource, properties, settings))
  }

  def write(values: Seq[String]): Unit = {
    writerOpt match {
      case Some(writer) => writer.writeLine(values)
      case None => throw new IllegalStateException("Tried to write to CSV Sink that has not been opened.")
    }
  }

  def closeTable()(implicit userContext: UserContext): Unit = {
    for(writer <- writerOpt) {
      writer.close()
    }
    writerOpt = None
  }

  override def close()(implicit userContext: UserContext): Unit = {
    super.close()
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    if(settings.clearBeforeExecution || force) {
      val resourceFile = new File(resource.path).getAbsoluteFile
      val resourcePath = resourceFile.toPath
      val crcFile = new File(resourcePath.getParent.toFile, s".${resourcePath.getFileName.toString}.crc")
      resource.delete()
      // Delete CRC file if exists
      try {
        if (crcFile.exists() && crcFile.canWrite && crcFile.isFile) {
          crcFile.delete()
        }
      } catch {
        case e: IOException =>
          log.warning("IO exception occurred when deleting CRC file: " + e.getMessage)
      }
    }
  }
}
