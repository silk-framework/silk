package org.silkframework.runtime.resource

import org.silkframework.config.DefaultConfig

import java.io.{ByteArrayOutputStream, InputStream}
import java.time.Instant
import java.util.logging.Logger
import scala.io.{Codec, Source}

/**
 * A resource, such as a file.
 */
trait Resource {

  protected lazy val log: Logger = Logger.getLogger(getClass.getName)

  /**
   * The local name of this resource.
   */
  def name: String

  /**
   * The path of this resource.
   */
  def path: String

  /**
    * Checks if this resource exists.
    */
  def exists: Boolean

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  def size: Option[Long]

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  def modificationTime: Option[Instant]

  /**
   * Creates an input stream for reading the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def inputStream: InputStream

  /**
    * Reads the input stream with a provided read function.
    * This method should usually be preferred over requesting an inputStream as it takes care of closing the stream after reading is done.
    */
  def read[T](reader: InputStream => T): T = {
    val is = inputStream
    try {
      reader(is)
    } finally {
      is.close()
    }
  }

  /**
   * Loads this resource into a string.
   */
  def loadAsString(implicit codec: Codec): String = {
    checkSizeForInMemory()
    val source = Source.fromInputStream(inputStream)(codec)
    try {
      source.getLines.mkString("\n")
    } finally {
      source.close()
    }
  }

  /**
    * Loads all lines of this resource into a sequence.
    */
  def loadLines(implicit codec: Codec): Seq[String] = {
    checkSizeForInMemory()
    val source = Source.fromInputStream(inputStream)(codec)
    try {
      source.getLines.toList
    } finally {
      source.close()
    }
  }

  /**
    * Loads this resource into a byte array.
    */
  def loadAsBytes: Array[Byte] = {
    checkSizeForInMemory()
    val in = inputStream
    try {
      val out = new ByteArrayOutputStream()
      var b = in.read()
      while (b > -1) {
        out.write(b)
        b = in.read()
      }
      out.toByteArray
    } finally {
      in.close()
    }
  }

  /**
    * True, if this resource does exist and is not empty.
    * False, otherwise.
    */
  def nonEmpty: Boolean = {
    if(exists) {
      size match {
        case Some(s) =>
          s > 0
        case None =>
          val in = inputStream
          try {
            in.read() != -1
          } finally {
            in.close()
          }
      }
    } else {
      false
    }
  }

  /**
   * Returns the name of this resource.
   */
  override def toString: String = name

  /**
    * Checks if this resource is not too large to be loaded into memory.
    * Called by all methods that load the resource contents into memory.
    *
    * @throws ResourceTooLargeException If this resource is too large to be loaded into memory.
    */
  def checkSizeForInMemory(): Unit = {
    size match {
      case Some(s) =>
        if(s > Resource.maxInMemorySize) {
          throw new ResourceTooLargeException(s"Resource $name is too large to be loaded into memory (size: $s, maximum size: ${Resource.maxInMemorySize}). " +
            s"Configure '${classOf[Resource].getName}.maxInMemorySize' in order to increase this limit.")
        }
      case None =>
        log.warning(s"Could not determine size of resource $name for loading contents into memory.")
    }
  }
}

object Resource {

  /**
    * Maximum resource size in bytes that should be loaded into memory.
    */
  lazy val maxInMemorySize: Long = {
    DefaultConfig.instance.forClass(classOf[Resource]).getMemorySize("maxInMemorySize").toBytes
  }

}