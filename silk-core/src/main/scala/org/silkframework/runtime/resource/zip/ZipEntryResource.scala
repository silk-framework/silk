package org.silkframework.runtime.resource.zip

import java.io.InputStream
import java.time.Instant
import java.util.zip.ZipEntry

import org.silkframework.runtime.resource.{Resource, ResourceWithKnownTypes}

/**
  * A resource that represents a Zip file entry.
  */
class ZipEntryResource private[zip](zipEntry: ZipEntry, resourceLoader: ZipInputStreamResourceLoader)
    extends Resource
        with ResourceWithKnownTypes {

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
    val z = resourceLoader.zip()
    while(z.getNextEntry.getName != zipEntry.getName && z.available() > 0){}
    z
  }

  override def knownTypes: IndexedSeq[String] = ZipEntryResource.getTypeAnnotation(zipEntry).toIndexedSeq
}

object ZipEntryResource{
  final val TYPE_URI_PREAMBLE = "Type URI: "

  def getTypeAnnotation(zipEntry: ZipEntry): Option[String] = {
    if(zipEntry.getComment != null && zipEntry.getComment.startsWith(ZipEntryResource.TYPE_URI_PREAMBLE)) {
      Some(zipEntry.getComment.drop(ZipEntryResource.TYPE_URI_PREAMBLE.length))
    } else if(zipEntry.getExtra != null && new String(zipEntry.getExtra).startsWith(ZipEntryResource.TYPE_URI_PREAMBLE)) {
      Some(new String(zipEntry.getExtra).drop(ZipEntryResource.TYPE_URI_PREAMBLE.length))
    } else {
      None
    }
  }
}