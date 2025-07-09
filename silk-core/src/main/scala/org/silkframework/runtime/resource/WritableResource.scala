package org.silkframework.runtime.resource

import org.silkframework.util.StreamUtils

import java.io.{File, FileInputStream, InputStream, OutputStream}
import scala.io.Codec

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
  def write[R](append: Boolean = false)(write: OutputStream => R): R = {
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
