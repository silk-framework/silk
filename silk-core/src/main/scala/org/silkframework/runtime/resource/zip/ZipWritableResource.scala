package org.silkframework.runtime.resource.zip

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.silkframework.runtime.resource.WritableResource


/**
  * Writes a single entry to the zip output stream.
  *
  * @param name - the name of the entry
  * @param closeEntriesAutomatically - if true, each write call will create a new entry, else the caller has to manage entries
  */
case class ZipWritableResource(
  name: String,
  path: String,
  zip: ZipOutputStream,
  closeEntriesAutomatically: Boolean = true
) extends WritableResource {

  /**
    * Creates an output stream for writing to this resource.
    * The caller is responsible for closing the stream after writing.
    * Using [[write()]] is preferred as it takes care of closing the output stream.
    */
  def createOutputStream(append: Boolean = false): OutputStream = {
    if(closeEntriesAutomatically)
      zip.putNextEntry(new ZipEntry(path))
    zip
  }

  override def exists: Boolean = false

  override def size: Option[Long] = None

  override def modificationTime: Option[Instant] = None

  override def delete(): Unit = throw ZipDeleteException()

  override def inputStream: InputStream = throw ZipReadException()
}

case class ZipReadException() extends UnsupportedOperationException(s"${classOf[ZipWritableResource]} does not support any read operations.")

case class ZipDeleteException() extends UnsupportedOperationException(s"${classOf[ZipWritableResource]} does not support deleting resources.")
