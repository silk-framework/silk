package org.silkframework.runtime.resource.zip

import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.resource._

import java.io.{BufferedInputStream, File}
import java.util.zip.{ZipEntry, ZipInputStream}
import scala.util.matching.Regex

/**
  * A resource iterator on a ZIP input stream.
  *
  * If the input comes from a local zip file, use the [[ZipFileResourceLoader]] instead.
  *
  * @param zip      A factory method to (re-)create the ZIP input stream.
 *  @param zipPath  The path of the ZIP file itself.
  * @param basePath The base path inside the zip from which the resources are loaded.
  *                 If empty, the resources from the root are loaded.
  */
case class ZipInputStreamResourceIterator(private[zip] val zip: () => ZipInputStream, zipPath: String, basePath: String = "") {

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
  def iterateReadOnceResources(filterRegex: Regex): CloseableIterator[Resource] = {
    val zipInputStream = ZipInputStreamResourceIterator.this.zip()
    // The entries are read strictly sequentially, so all of them can share one probe buffer
    val head = new Array[Byte](maxCompressedSizeForInMemory + 1)
    var currentResource: Option[WritableResource with ResourceWithKnownTypes] = None
    val iterator =
      ZipInputStreamResourceIterator.listEntries(zipInputStream)
                                    .filter(entry => !entry.isDirectory && filterRegex.findFirstIn(entry.getName).isDefined)
                                    .map { entry =>
        val tempResource = createCompressedResource(entry, zipInputStream, head)
        currentResource.foreach(_.delete())
        currentResource = Some(tempResource)
        tempResource
      }
    CloseableIterator(iterator, zipInputStream).thenClose(() => currentResource.foreach(_.delete()))
  }

  // Creates a compressed, in-memory or file based resource from the ZIP input stream.
  // The head buffer is used to probe the entry size and must hold maxCompressedSizeForInMemory + 1 bytes.
  private def createCompressedResource(entry: ZipEntry, z: ZipInputStream, head: Array[Byte]): WritableResource with ResourceWithKnownTypes = {
    // ZipEntry.getCompressedSize is -1 for entries that have been written in streaming mode (as ZipOutputStream does, including
    // our own exports), which would make every entry look small enough for memory. So the entry is read up to the limit instead and
    // only the remainder decides whether it has to be spilled to a temp file.
    var headSize = 0
    var lastRead = 0
    while (lastRead >= 0 && headSize < head.length) {
      lastRead = z.read(head, headSize, head.length - headSize)
      if (lastRead > 0) {
        headSize += lastRead
      }
    }
    val fitsInMemory = headSize <= maxCompressedSizeForInMemory
    val r = if (fitsInMemory) {
      CompressedInMemoryResource(entry.getName, zipPath, Some(entry.getName), ZipEntryUtil.getTypeAnnotation(entry).toIndexedSeq)
    } else {
      val tempFile = File.createTempFile("zipResource", ".bin")
      tempFile.deleteOnExit()
      // Since there is no way to know when the last resource will not be used anymore, we set the deleteOnGC flag, so it gets eventually deleted.
      CompressedFileResource(tempFile, entry.getName, zipPath, Some(entry.getName), ZipEntryUtil.getTypeAnnotation(entry).toIndexedSeq, deleteOnGC = true)
    }
    r.write() { outputStream =>
      outputStream.write(head, 0, headSize)
      if (!fitsInMemory) {
        z.transferTo(outputStream)
      }
    }
    r
  }
}

object ZipInputStreamResourceIterator{

  def apply(resource: Resource, basePath: String): ZipInputStreamResourceIterator = {
    apply(() => new ZipInputStream(new BufferedInputStream(resource.inputStream)), resource.path, basePath)
  }

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