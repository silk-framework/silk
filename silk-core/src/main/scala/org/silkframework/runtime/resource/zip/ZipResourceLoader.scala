package org.silkframework.runtime.resource.zip

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipInputStream}

import org.silkframework.runtime.resource.{Resource, ResourceLoader, ResourceNotFoundException}

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

  /** Iterate over all resource, but only allow reading of each resource only once. Reading a resource more than once
    * will lead to an [[IllegalStateException]] being thrown. */
  def iterateReadOnceResources(): Traversable[ReadOnceZipResource] = {
    val thisResourceLoader = this
    new Traversable[ReadOnceZipResource] {
      override def foreach[U](f: ReadOnceZipResource => U): Unit = {
        val z = zip()
        try {
          ZipResourceLoader.listEntries(z) foreach { entry =>
            f(new ReadOnceZipResource(new ZipEntryResource(entry, thisResourceLoader), entry, z))
          }
        } finally {
          z.close()
        }
      }
    }
  }

  /** Version of ZipEntryResource that can only be read once. This only reads the ZIP file once and should be used when performance matters. */
  class ReadOnceZipResource(zipEntryResource: ZipEntryResource, zipEntry: ZipEntry, is: ZipInputStream) extends Resource {
    override def name: String = zipEntryResource.name

    override def path: String = zipEntryResource.path

    override def exists: Boolean = zipEntryResource.exists

    override def size: Option[Long] = zipEntryResource.size

    override def modificationTime: Option[Instant] = zipEntryResource.modificationTime

    private var inputStreamFetched = false

    override def inputStream: InputStream = {
      // Optimization to the ZipEntryResource.inputStream method which reads the whole ZIP file. This will use the ZipInputStream directly, but only once.
      if(inputStreamFetched) {
        throw new IllegalStateException("InputStream has already been consumed!")
      } else {
        inputStreamFetched = true
        new InputStream {
          override def read(): Int = is.read()

          override def available(): Int = is.available()

          override def close(): Unit = { }
        }
      }
    }
  }
}

object ZipResourceLoader{

  def apply(file: File, basePath: String): ZipResourceLoader = apply(() => new ZipInputStream(new BufferedInputStream(new FileInputStream(file))), basePath)

  def apply(resource: Resource, basePath: String): ZipResourceLoader = apply(() => new ZipInputStream(new BufferedInputStream(resource.inputStream)), basePath)

  def listEntries(stream: ZipInputStream): Iterator[ZipEntry] = new Iterator[ZipEntry] {
    private var nextEntry = stream.getNextEntry
    override def hasNext: Boolean = nextEntry != null

    override def next(): ZipEntry = {
      val zw = nextEntry
      nextEntry = stream.getNextEntry
      zw
    }
  }
}
