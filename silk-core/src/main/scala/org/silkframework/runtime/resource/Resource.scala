package org.silkframework.runtime.resource

import java.io.{ByteArrayOutputStream, InputStream}

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
   * Loads the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def load: InputStream

  /**
   * Loads this resource into a string.
   */
  def loadAsString(implicit codec: Codec): String = {
    Source.fromInputStream(load)(codec).getLines.mkString("\n")
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
   * Returns the name of this resource.
   */
  override def toString = name
}
