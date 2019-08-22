package org.silkframework.runtime.resource.zip

import java.io.{BufferedInputStream, File, FileInputStream}
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
