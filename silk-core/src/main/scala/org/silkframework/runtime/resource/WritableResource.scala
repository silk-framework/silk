package org.silkframework.runtime.resource

import com.typesafe.config.ConfigException
import org.silkframework.config.DefaultConfig
import org.silkframework.util.StreamUtils

import java.io.{File, FileInputStream, InputStream, OutputStream}
import java.util.logging.Logger
import scala.io.Codec
import scala.util.control.NonFatal

trait WritableResource extends Resource {

  /**
    * Creates an output stream for writing to this resource.
    * The caller is responsible for closing the stream after writing.
    * Using [[write()]] is preferred as it takes care of closing the output stream.
    */
  def createOutputStream(append: Boolean = false): OutputStream

  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  def write(append: Boolean = false)(write: OutputStream => Unit): Unit = {
    val outputStream = createOutputStream(append)
    try {
      write(outputStream)
    } finally {
      outputStream.close()
    }
  }

  /**
    * Writes the contents of a provided input stream.
    * Does not close the input stream.
    */
  def writeStream(inputStream: InputStream, append: Boolean = false, closeStream: Boolean = false): Unit = {
    try {
      write(append) { outputStream =>
        StreamUtils.fastStreamCopy(inputStream, outputStream, close = false)
      }
    } finally {
      if(closeStream) {
        inputStream.close()
      }
    }
  }

  /**
    * Writes a file.
    */
  def writeFile(file: File): Unit = {
    writeStream(new FileInputStream(file), closeStream = true)
  }

  /**
    * Writes the contents of another resource.
    */
  def writeResource(res: Resource, append: Boolean = false): Unit = {
    res.read(is => writeStream(is, append))
  }


  /**
    * Writes raw bytes.
    */
  def writeBytes(bytes: Array[Byte], append: Boolean = false): Unit = {
    write(append) { os =>
      os.write(bytes)
    }
  }

  /**
    * Writes a string.
    */
  def writeString(content: String, append: Boolean = false, codec: Codec = Codec.UTF8): Unit = {
    write(append) { os =>
      os.write(content.getBytes(codec.charSet))
    }
  }

  /**
    * Deletes this resource.
    */
  def delete(): Unit

}

object WritableResource {
  private val log: Logger = Logger.getLogger(getClass.getName)
  final val fileSystemFreeSpaceThresholdKey = "config.production.localFileSystemFreeSpaceThreshold"

  class WritableResourceException(msg: String) extends RuntimeException(msg)

  /** Checks if there is enough free space left on the file system the file resides on. */
  def checkFreeSpace(file: File, threshold: Option[Long]): Unit = {
    threshold foreach { limit =>
      def checkRecursive(file: File, threshold: Option[Long]): Unit = {

        // We can only get FS stats from existing files, so we need to recursively go up until a parent directory exists
        if (!file.exists()) {
          Option(file.getParentFile).foreach(parent => checkFreeSpace(parent, threshold))
        } else {
          val freeSpace = file.getUsableSpace
          if (freeSpace < limit) {
            throw new WritableResourceException(s"Cannot write to file '${file.getName}'. Free space of $freeSpace is less than the configured" +
              s" minimal value of $limit. You can change the threshold via " +
              s"config parameter '${WritableResource.fileSystemFreeSpaceThresholdKey}'.")
          }
        }
      }
      checkRecursive(file.getAbsoluteFile, threshold)
    }
  }

  def retrieveFreeSpaceThreshold(): Option[Long] = {
    val cfg = DefaultConfig.instance()
    try {
      Some(cfg.getMemorySize(fileSystemFreeSpaceThresholdKey).toBytes)
    } catch {
      case _: ConfigException.Missing =>
        None
      case ex: ConfigException =>
        log.warning(s"Cannot read value of config parameter '$fileSystemFreeSpaceThresholdKey' to configure free space threshold. Details: ${ex.getMessage}")
        None
      case NonFatal(_) =>
        None
    }
  }

  lazy val freeSpaceThreshold: Option[Long] = retrieveFreeSpaceThreshold()
}