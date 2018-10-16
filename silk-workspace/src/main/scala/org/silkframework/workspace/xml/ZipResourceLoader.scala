package org.silkframework.workspace.xml

import java.io.InputStream
import java.time.Instant
import java.util.zip.{ZipEntry, ZipFile}

import org.silkframework.runtime.resource.{Resource, ResourceLoader, ResourceNotFoundException}

import scala.collection.JavaConversions._

case class ZipResourceLoader(zip: ZipFile, basePath: String = "") extends ResourceLoader {

  /**
    * Lists all available files at the given base path.
    */
  override def list: List[String] = {
    val filesBelowBasePath = zip.entries.toList.filterNot(_.isDirectory).filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    filesBelowBasePath.filterNot(_.contains("/"))
  }

  /**
    * Lists all available directories at the given base path.
    */
  override def listChildren: List[String] = {
    val entriesBelowBasePath = zip.entries.toList.filter(_.getName.startsWith(basePath)).map(_.getName.stripPrefix(basePath + "/"))
    val localNames = entriesBelowBasePath.filter(_.contains("/")).map(_.takeWhile(_ != '/'))
    localNames.distinct
  }

  /**
    * Retrieves a file at the given base path.
    */
  override def get(name: String, mustExist: Boolean): Resource = {
    val zipEntry = zip.getEntry(fullPath(name))
    if(zipEntry == null) {
      // If the zip entry is not found, we fail fast even if mustExist is false.
      throw new ResourceNotFoundException(s"No resource $name found in zip file at path $basePath.")
    }
    ZipEntryResource(zipEntry)
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
}
