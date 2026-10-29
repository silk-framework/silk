package org.silkframework.runtime.resource.zip

import org.silkframework.runtime.resource.{ResourceNotFoundException, WritableResource}

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.util.control.NonFatal

/**
 * A resource representing an entry inside a zip file.
 *
 * @param zipFile The zip file.
 * @param zipEntry The entry path inside the zip file.
 */
class ZipResource(zipFile: WritableResource, zipEntry: String) extends WritableResource {

  override val entryPath: Option[String] = Some(zipEntry)

  override def name: String = {
    // Extract the name from the entry path, which is the last part after the last slash
    zipEntry.split("/").lastOption.getOrElse("")
  }

  /**
   * The path of this resource.
   */
  override def path: String = zipFile.path

  /**
   * Checks if this resource exists.
   */
  override def exists: Boolean = {
    if(zipFile.exists) {
      val zipInputStream = new ZipInputStream(zipFile.inputStream)
      try {
        var entry = zipInputStream.getNextEntry
        while (entry != null) {
          if (entry.getName == zipEntry) {
            return true
          }
          entry = zipInputStream.getNextEntry
        }
        false
      } catch {
        case _: Exception => false // If reading fails, assume it does not exist
      } finally {
        zipInputStream.close()
      }
    } else {
      false
    }
  }

  /**
   * Returns the size of this resource in bytes.
   * Returns None if the size is not known.
   */
  override def size: Option[Long] = {
    None
  }

  /**
   * The time that the resource was last modified.
   * Returns None if the time is not known.
   */
  override def modificationTime: Option[Instant] = {
    None
  }

  /**
   * Creates an input stream for reading the zip entry.
   */
  override def inputStream: InputStream = {
    val zipInputStream = new ZipInputStream(zipFile.inputStream)
    try {
      var entry = zipInputStream.getNextEntry
      while (entry != null) {
        if (entry.getName == zipEntry) {
          return zipInputStream // The caller takes ownership of the stream and has to close it
        }
        entry = zipInputStream.getNextEntry
      }
      throw new ResourceNotFoundException(s"Entry '$zipEntry' not found in zip file ${zipFile.name}.")
    } catch {
      case NonFatal(ex) =>
        // The caller never receives the stream on this path, so it has to be closed here to not leak the file handle
        try {
          zipInputStream.close()
        } catch {
          case NonFatal(closeEx) => ex.addSuppressed(closeEx)
        }
        throw ex
    }
  }

  /**
   * Creates an output stream for writing to the zip entry.
   */
  override def createOutputStream(append: Boolean): OutputStream = {
    if (append) {
      throw new UnsupportedOperationException("Appending to a zip resource is not supported.")
    } else {
      val zipOutputStream = new ZipOutputStream(zipFile.createOutputStream(append = false))
      zipOutputStream.putNextEntry(new ZipEntry(zipEntry))
      new OutputStream {
        override def write(b: Int): Unit = zipOutputStream.write(b)
        override def write(b: Array[Byte]): Unit = zipOutputStream.write(b)
        override def write(b: Array[Byte], off: Int, len: Int): Unit = zipOutputStream.write(b, off, len)
        override def flush(): Unit = zipOutputStream.flush()
        override def close(): Unit = {
          zipOutputStream.closeEntry()
          zipOutputStream.close()
        }
      }
    }
  }

  /**
   * Deletes this resource.
   */
  override def delete(): Unit = {
    throw new UnsupportedOperationException("Deleting entries from a zip file is not supported.")
  }
}
