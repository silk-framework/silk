package org.silkframework.runtime.resource.zip

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipInputStream}

import org.silkframework.runtime.resource.{CompressedFileResource, CompressedInMemoryResource, Resource, ResourceLoader, ResourceNotFoundException, ResourceWithKnownTypes, WritableResource}

import scala.util.matching.Regex

/**
  * A resource loader that loads all resources from a zip file.
  *
  * @param zip The zip file to read all resources from.
  * @param basePath The based path inside the zip from which the resources are loaded.
  *                 If empty, the resources from the root are loaded.
  */
case class ZipResourceLoader(private[zip] val zip: () => ZipInputStream, basePath: String = "") extends ResourceLoader {

  /**
    * Lists all available files at the given base path.
    */
  override def list: List[String] = {
    val z = zip()
    val filesBelowBasePath = ZipResourceLoader.listEntries(z).toList.filterNot(_.isDirectory).filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    z.close()
    filesBelowBasePath.filterNot(_.contains("/"))
  }

  /**
    * Lists all available directories at the given base path.
    */
  override def listChildren: List[String] = {
    val z = zip()
    val entriesBelowBasePath = ZipResourceLoader.listEntries(z).toList.filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    val localNames = entriesBelowBasePath.filter(_.contains("/")).map(_.takeWhile(_ != '/'))
    z.close()
    localNames.distinct
  }

  /**
    * Retrieves a file at the given base path.
    */
  override def get(name: String, mustExist: Boolean): Resource = {
    val z = zip()
    ZipResourceLoader.listEntries(z).find(e => e.getName == fullPath(name)) match{
      case Some(e) =>
        z.close()
        new ZipEntryResource(e, this)
      case None =>
        z.close()
        throw new ResourceNotFoundException(s"No resource $name found in zip file at path $basePath.") // If the zip entry is not found, we fail fast even if mustExist is false.
    }
  }

  override def child(name: String): ResourceLoader = ZipResourceLoader(zip, fullPath(name))

  override def parent: Option[ResourceLoader] = {
    val slashIndex = basePath.lastIndexOf('/')
    if(slashIndex == -1) {
      None
    } else {
      Some(ZipResourceLoader(zip, basePath.substring(0, slashIndex)))
    }
  }

  private def fullPath(name: String) = {
    if(basePath.isEmpty) {
      name
    } else {
      basePath + "/" + name
    }
  }

  /* The max size of the ZIP resource that it will be kept in memory when iterating over the resources.
     This should avoid file access overhead for many small resources. The actual compressed size if the resource might be larger,
     since we use a different compression algorithm that is optimized for write and especially read performance.
     */
  final val maxCompressedSizeForInMemory = 64 * 1000 // 64KB

  /** Iterate over all resources, but only allow reading of the current resource repeatedly.
    * The current resource will be persisted either in-memory of on disk while it is available.
    *
    * @param filterRegex A regex to filter resources by their path value.
    **/
  def iterateReadOnceResources(filterRegex: Regex): Traversable[Resource] = {
    new Traversable[Resource] {
      override def foreach[U](f: Resource => U): Unit = {
        val zipInputStream = zip()
        var currentResource: Option[WritableResource with ResourceWithKnownTypes] = None
        try {
          ZipResourceLoader.listEntries(zipInputStream) foreach { entry =>
            if (!entry.isDirectory && filterRegex.findFirstIn(entry.getName).isDefined) {
              val tempResource = createCompressedResource(entry, zipInputStream)
              currentResource.foreach(_.delete())
              currentResource = Some(tempResource)
              f(tempResource)
            }
          }
        } finally {
          zipInputStream.close()
//          currentResource.foreach(_.delete())  TODO: How to handle last resource if it is a file?
        }
      }
    }
  }

  // Creates a compressed in-memory of file base resource from the ZIP input stream.
  private def createCompressedResource[U](entry: ZipEntry, z: ZipInputStream): WritableResource with ResourceWithKnownTypes = {
    val r = if (entry.getCompressedSize <= maxCompressedSizeForInMemory) {
      CompressedInMemoryResource(entry.getName, entry.getName, ZipEntryResource.getTypeAnnotation(entry).toIndexedSeq)
    } else {
      val tempFile = File.createTempFile("zipResource", ".bin")
      tempFile.deleteOnExit()
      CompressedFileResource(tempFile, entry.getName, entry.getName, ZipEntryResource.getTypeAnnotation(entry).toIndexedSeq)
    }
    r.writeStream(z)
    r
  }
}

object ZipResourceLoader{

  def apply(file: File, basePath: String): ZipResourceLoader = apply(() => new ZipInputStream(new BufferedInputStream(new FileInputStream(file))), basePath)

  def apply(resource: Resource, basePath: String): ZipResourceLoader = apply(() => new ZipInputStream(new BufferedInputStream(resource.inputStream)), basePath)

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
