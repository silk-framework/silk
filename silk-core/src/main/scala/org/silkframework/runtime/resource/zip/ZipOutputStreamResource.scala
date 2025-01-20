package org.silkframework.runtime.resource.zip

import org.silkframework.runtime.resource.WritableResource

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipOutputStream}


/**
  * Writes a single entry to the zip output stream.
  *
  * @param name - the name of the entry
  * @param closeEntriesAutomatically - if true, each write call will create a new entry, else the caller has to manage entries
  */
case class ZipOutputStreamResource(
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
    if(closeEntriesAutomatically) {
      zip.putNextEntry(new ZipEntry(path))
    }
    new NonClosingOutputStream(zip)
  }

  override def exists: Boolean = false

  override def size: Option[Long] = None

  override def modificationTime: Option[Instant] = None

  override def delete(): Unit = throw ZipDeleteException()

  override def inputStream: InputStream = throw ZipReadException()

  // An output stream wrapper that does not close the underlying stream on close.
  private class NonClosingOutputStream(os: OutputStream) extends OutputStream {
    override def write(b: Int): Unit = os.write(b)
    override def write(b: Array[Byte]): Unit = os.write(b)
    override def write(b: Array[Byte], off: Int, len: Int): Unit = os.write(b, off, len)
    override def flush(): Unit = os.flush()
    override def close(): Unit = {
      // do not close the underlying stream
    }
  }
}

case class ZipReadException() extends UnsupportedOperationException(s"${classOf[ZipOutputStreamResource]} does not support any read operations.")

case class ZipDeleteException() extends UnsupportedOperationException(s"${classOf[ZipOutputStreamResource]} does not support deleting resources.")
