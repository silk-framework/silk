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

  override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
    if(closeEntriesAutomatically)
      zip.putNextEntry(new ZipEntry(path))
    write(zip)
  }

  override def exists: Boolean = false

  override def size: Option[Long] = None

  override def modificationTime: Option[Instant] = None

  override def delete(): Unit = throw ZipDeleteException()

  override def inputStream: InputStream = throw ZipReadException()
}

case class ZipReadException() extends UnsupportedOperationException(s"${classOf[ZipWritableResource]} does not support any read operations.")

case class ZipDeleteException() extends UnsupportedOperationException(s"${classOf[ZipWritableResource]} does not support deleting resources.")
