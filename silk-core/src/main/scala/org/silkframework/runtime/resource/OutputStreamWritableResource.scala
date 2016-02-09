package org.silkframework.runtime.resource

import java.io.{IOException, InputStream, OutputStream}

/**
 * Created by andreas on 1/27/16.
 */
case class OutputStreamWritableResource(outputStream: OutputStream) extends WritableResource {
  /**
   * Preferred method for writing to a resource.
   *
   * @param write A function that accepts an output stream and writes to it.
   */
  override def write(write: (OutputStream) => Unit): Unit = {
    write(outputStream)
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
  override def load: InputStream = {
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
}
