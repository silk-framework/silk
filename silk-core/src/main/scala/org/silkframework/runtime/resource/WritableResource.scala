package org.silkframework.runtime.resource

import java.io.{File, FileInputStream, InputStream, OutputStream}

trait WritableResource extends Resource {

  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  def write(append: Boolean = false)(write: OutputStream => Unit)

  /**
    * Writes the contents of a provided input stream.
    * Does not close the input stream.
    */
  def writeStream(inputStream: InputStream, append: Boolean = false, closeStream: Boolean = false): Unit = {
    try {
      write(append) { outputStream =>
        var b = inputStream.read()
        while (b != -1) {
          outputStream.write(b)
          b = inputStream.read()
        }
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
  def writeString(content: String, append: Boolean = false): Unit = {
    write(append) { os =>
      os.write(content.getBytes("UTF-8"))
    }
  }

  /**
    * Deletes this resource.
    */
  def delete(): Unit

}
