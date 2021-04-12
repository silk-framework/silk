package org.silkframework.runtime.resource

import java.io.{IOException, InputStream, OutputStream}

/**
 * Created by andreas on 1/27/16.
 */
case class OutputStreamWritableResource(outputStream: OutputStream) extends WritableResource {

  /**
    * Creates an output stream for writing to this resource.
    * The caller is responsible for closing the stream after writing.
    * Using [[write()]] is preferred as it takes care of closing the output stream.
    */
  def createOutputStream(append: Boolean = false): OutputStream = {
    outputStream
  }

  /**
   * The local name of this resource.
   */
  override def name: String = {
    "OutputStreamWritableResource"
  }

  /**
   * Loads the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  override def inputStream: InputStream = {
    throw new IOException("load: OutputStreamWritableResource cannot be read!")
  }

  /**
   * The path of this resource.
   */
  override def path: String = {
    ""
  }

  /**
   * Checks if this resource exists.
   */
  override def exists: Boolean = {
    outputStream != null
  }

  override def size = None

  override def modificationTime = None

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = {
  }
}
