package org.silkframework.runtime.resource.zip

import org.silkframework.runtime.resource.{Resource, ResourceLoader, ResourceNotFoundException}

import java.io.{File, InputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipFile}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

/**
  * A resource loader that loads all resources from a zip file.
  *
  * @param zip The zip file to read all resources from.
  * @param basePath The based path inside the zip from which the resources are loaded.
  *                 If empty, the resources from the root are loaded.
  */
case class ZipFileResourceLoader(zip: ZipFile, basePath: String) extends ResourceLoader {

  /**
    * Lists all available files at the given base path.
    */
  override def list: List[String] = {
    val filesBelowBasePath = zip.entries.asScala.toList.filterNot(_.isDirectory).filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    filesBelowBasePath.filterNot(_.contains("/"))
  }

  /**
    * Lists all available directories at the given base path.
    */
  override def listChildren: List[String] = {
    val entriesBelowBasePath = zip.entries.asScala.toList.filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    val localNames = entriesBelowBasePath.filter(_.contains("/")).map(_.takeWhile(_ != '/'))
    localNames.distinct
  }

  /**
    * Retrieves a file at the given base path.
    */
  override def get(name: String, mustExist: Boolean): Resource = {
    val zipEntry = zip.getEntry(fullPath(name))
    if(zipEntry != null) {
      ZipEntryResource(zipEntry)
    } else if(mustExist) {
      throw new ResourceNotFoundException(s"No resource $name found in zip file at path $basePath.")
    } else {
      MissingResource(name, fullPath(name))
    }
  }

  override def child(name: String): ResourceLoader = ZipFileResourceLoader(zip, fullPath(name))

  override def parent: Option[ResourceLoader] = {
    val slashIndex = basePath.lastIndexOf('/')
    if(slashIndex == -1) {
      None
    } else {
      Some(ZipFileResourceLoader(zip, basePath.substring(0, slashIndex)))
    }
  }

  private def fullPath(name: String) = {
    if(basePath.isEmpty) {
      name
    } else {
      basePath + "/" + name
    }
  }

  /**
    * A resource that represents a Zip file entry.
    */
  case class ZipEntryResource(zipEntry: ZipEntry) extends Resource {

    /**
      * The local name of this resource.
      */
    override def name: String = zipEntry.getName.reverse.takeWhile(_ != '/').reverse

    /**
      * The path of this resource.
      */
    override def path: String = zipEntry.getName

    /**
      * Checks if this resource exists.
      */
    override def exists: Boolean = true

    /**
      * Returns the size of this resource in bytes.
      * Returns None if the size is not known.
      */
    override def size: Option[Long] = zipEntry.getSize match {
      case -1 => None
      case size => Some(size)
    }

    /**
      * The time that the resource was last modified.
      * Returns None if the time is not known.
      */
    override def modificationTime: Option[Instant] = {
      Option(zipEntry.getLastModifiedTime).map(_.toInstant)
    }

    /**
      * Creates an input stream for reading the resource.
      *
      * @return An input stream for reading the resource.
      *         The caller is responsible for closing the stream after reading.
      */
    override def inputStream: InputStream = {
      zip.getInputStream(zipEntry)
    }

  }

  /**
    * Returned if the user requests a non-existing resource, but mustExist is false.
    */
  case class MissingResource(name: String, path: String) extends Resource {
    override def exists: Boolean = false
    override def size: Option[Long] = None
    override def modificationTime: Option[Instant] = None
    override def inputStream: InputStream = {
      throw new ResourceNotFoundException(s"No resource $name found in zip file at path $basePath.")
    }
  }
}

object ZipFileResourceLoader {
  def apply(file: File, basePath: String): ZipFileResourceLoader = {
    ZipFileResourceLoader(new ZipFile(file), basePath)
  }

  def apply(file: File): ZipFileResourceLoader = {
    apply(file, basePath = "")
  }

  def apply(zipFile: ZipFile): ZipFileResourceLoader = {
    apply(zipFile, basePath = "")
  }
}