package org.silkframework.runtime.resource

import java.io.{InputStream, OutputStream}

trait WritableResource extends Resource{

  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  def write(write: OutputStream => Unit)

  /**
    * Writes the contents of a provided input stream.
    * Does not close the input stream.
    */
  def write(inputStream: InputStream) {
    write { outputStream =>
      var b = inputStream.read()
      while(b != -1) {
        outputStream.write(b)
        b = inputStream.read()
      }
    }
  }

  /**
    * Writes a string.
    */
  def write(content: String): Unit = {
    write(os => os.write(content.getBytes("UTF-8")))
  }

}
