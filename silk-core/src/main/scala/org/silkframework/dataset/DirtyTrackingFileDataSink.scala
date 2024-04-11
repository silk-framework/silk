package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import scala.jdk.CollectionConverters.CollectionHasAsScala

/** Tracks updated files written to by this file-based sink. */
trait DirtyTrackingFileDataSink extends DataSink {
  protected def resource: WritableResource

  abstract override def close()(implicit userContext: UserContext): Unit = {
    DirtyTrackingFileDataSink.addUpdatedFile(resource.name)
    super.close()
  }
}

object DirtyTrackingFileDataSink {
  private val updatedFiles = new ConcurrentHashMap[String, String]()
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def fetchAndClearUpdatedFiles(): Set[String] = this.synchronized {
    val currentEntries = updatedFiles.values().asScala
    val entries: Set[String] = Set.empty ++ currentEntries
    updatedFiles.clear()
    entries
  }

  def addUpdatedFile(resourceName: String): Unit = this.synchronized {
    log.fine(s"File $resourceName has been updated!")
    updatedFiles.put(resourceName, resourceName)
  }
}