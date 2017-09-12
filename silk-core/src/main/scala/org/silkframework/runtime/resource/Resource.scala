package org.silkframework.runtime.resource

import java.io.{ByteArrayOutputStream, InputStream}
import java.time.Instant

import scala.io.{Codec, Source}

/**
 * A resource, such as a file, which is required by a plugin.
 */
trait Resource {

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
   * Loads the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def load: InputStream

  /**
    * Reads the input stream with a provided read function.
    * This method should usually be preferred over load() as it takes care of closing the input stream after reading is done.
    */
  def read[T](reader: InputStream => T): T = {
    val inputStream = load
    try {
      reader(inputStream)
    } finally {
      inputStream.close()
    }
  }

  /**
   * Loads this resource into a string.
   */
  def loadAsString(implicit codec: Codec): String = {
    val source = Source.fromInputStream(load)(codec)
    try {
      source.getLines.mkString("\n")
    } finally {
      source.close()
    }
  }

  /**
    * Loads this resource into a byte array.
    */
  def loadAsBytes: Array[Byte] = {
    val in = load
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
      val inputStream = load
      try {
        inputStream.read() != -1
      } finally {
        inputStream.close()
      }
    } else {
      false
    }
  }

  /**
   * Returns the name of this resource.
   */
  override def toString: String = name
}
