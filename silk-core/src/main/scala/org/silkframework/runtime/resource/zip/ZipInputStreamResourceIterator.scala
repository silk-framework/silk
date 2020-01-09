package org.silkframework.runtime.resource.zip

import java.io.{BufferedInputStream, File}
import java.util.zip.{ZipEntry, ZipInputStream}

import org.silkframework.runtime.resource._

import scala.util.matching.Regex

/**
  * A resource iterator on a ZIP input stream.
  *
  * If the input comes from a local zip file, use the [[ZipFileResourceLoader]] instead.
  *
  * @param zip      A factory method to (re-)create the ZIP input stream.
  * @param basePath The based path inside the zip from which the resources are loaded.
  *                 If empty, the resources from the root are loaded.
  */
case class ZipInputStreamResourceIterator(private[zip] val zip: () => ZipInputStream, basePath: String = "") {

  /* The max size of the ZIP resource, so that it will be kept in memory when iterating over the resources.
     This should avoid file access overhead for many small resources. The actual compressed size of the resource might be larger,
     since we use a different compression algorithm that is optimized for write and especially read performance.
     */
  final val maxCompressedSizeForInMemory = 64 * 1000 // 64KB

  /** Iterate over all resources, but only allow reading of the current resource repeatedly.
    * The current resource will be persisted either in-memory of on disk while it is available.
    * Caveat: Resources are deleted as soon as the next resource is requested.
    *
    * @param filterRegex A regex to filter resources by their path value.
    **/
  def iterateReadOnceResources(filterRegex: Regex): Traversable[Resource] = {
    new Traversable[Resource] {
      override def foreach[U](f: Resource => U): Unit = {
        val zipInputStream = zip()
        var currentResource: Option[WritableResource with ResourceWithKnownTypes] = None
        try {
          ZipInputStreamResourceIterator.listEntries(zipInputStream) foreach { entry =>
            if (!entry.isDirectory && filterRegex.findFirstIn(entry.getName).isDefined) {
              val tempResource = createCompressedResource(entry, zipInputStream)
              currentResource.foreach(_.delete())
              currentResource = Some(tempResource)
              f(tempResource)
            }
          }
        } finally {
          zipInputStream.close()
        }
      }
    }
  }

  // Creates a compressed, in-memory or file bases resource from the ZIP input stream.
  private def createCompressedResource[U](entry: ZipEntry, z: ZipInputStream): WritableResource with ResourceWithKnownTypes = {
    val r = if (entry.getCompressedSize <= maxCompressedSizeForInMemory) {
      CompressedInMemoryResource(entry.getName, entry.getName, ZipEntryUtil.getTypeAnnotation(entry).toIndexedSeq)
    } else {
      val tempFile = File.createTempFile("zipResource", ".bin")
      tempFile.deleteOnExit()
      // Since there is no way to know when the last resource will not be used anymore, we set the deleteOnGC flag, so it gets eventually deleted.
      CompressedFileResource(tempFile, entry.getName, entry.getName, ZipEntryUtil.getTypeAnnotation(entry).toIndexedSeq, deleteOnGC = true)
    }
    r.writeStream(z)
    r
  }
}

object ZipInputStreamResourceIterator{

  def apply(resource: Resource, basePath: String): ZipInputStreamResourceIterator = apply(() => new ZipInputStream(new BufferedInputStream(resource.inputStream)), basePath)

  def listEntries(stream: ZipInputStream): Iterator[ZipEntry] = new Iterator[ZipEntry] {
    private var nextEntry: ZipEntry = null
    private var fetchNext = true
    override def hasNext: Boolean = {
      fetchIfNecessary()
      nextEntry != null
    }

    override def next(): ZipEntry = {
      fetchIfNecessary()
      fetchNext = true
      nextEntry
    }

    private def fetchIfNecessary(): Unit = {
      if(fetchNext) {
        nextEntry = stream.getNextEntry
        fetchNext = false
      }
    }
  }
}

object ZipEntryUtil {
  final val TYPE_URI_PREAMBLE = "Type URI: "

  def getTypeAnnotation(zipEntry: ZipEntry): Option[String] = {
    if(zipEntry.getComment != null && zipEntry.getComment.startsWith(TYPE_URI_PREAMBLE)) {
      Some(zipEntry.getComment.drop(TYPE_URI_PREAMBLE.length))
    } else if(zipEntry.getExtra != null && new String(zipEntry.getExtra).startsWith(TYPE_URI_PREAMBLE)) {
      Some(new String(zipEntry.getExtra).drop(TYPE_URI_PREAMBLE.length))
    } else {
      None
    }
  }
}