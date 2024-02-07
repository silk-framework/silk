package org.silkframework.util

import java.io.{DataInput, DataOutput, IOException, InputStream, OutputStream}
import java.nio.{Buffer, ByteBuffer}
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets

/**
  * Utility methods for I/O stream handling.
  */
object StreamUtils {

  @throws[IOException]
  def fastChannelCopy(src: ReadableByteChannel, dest: WritableByteChannel): Unit = {
    val buffer = ByteBuffer.allocateDirect(16 * 1024)
    while (src.read(buffer) != -1) { // prepare the buffer to be drained
      // Casting to Buffer to avoid conflict mentioned here: https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip
      buffer.asInstanceOf[Buffer].flip()
      // write to the channel, may block
      dest.write(buffer)
      // If partial transfer, shift remainder down
      // If buffer is empty, same as doing clear()
      buffer.compact
    }
    // EOF will leave buffer in fill state
    // Casting to Buffer to avoid conflict mentioned here: https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip
    buffer.asInstanceOf[Buffer].flip()
    // make sure the buffer is fully drained.
    while (buffer.hasRemaining) {
      dest.write(buffer)
    }
  }

  def fastStreamCopy(input: InputStream, output: OutputStream, close: Boolean): Unit = {
    // get an channel from the stream
    val inputChannel = Channels.newChannel(input)
    val outputChannel = Channels.newChannel(output)
    // copy the channels
    fastChannelCopy(inputChannel, outputChannel)
    if(close) {
      // closing the channels
      inputChannel.close()
      outputChannel.close()
    }
  }

  /**
   * Writes a string into a DataOutput.
   * This is preferred over using writeUTF on the DataOutput itself because it supports strings larger than 65k
   */
  @inline
  def writeString(dataOutput: DataOutput, string: String): Unit = {
    // Convert the string into UTF8 bytes
    val stringBytes = string.getBytes(StandardCharsets.UTF_8)
    val length = stringBytes.length

    // Write the length of the string
    dataOutput.writeInt(length)

    // Write the actual string bytes
    dataOutput.write(stringBytes)
  }

  /**
   * Reads back a string that has been written by the corresponding `writeString` function.
   */
  @inline
  def readString(dataInput: DataInput): String = {
    // Read the length of the string
    val length = dataInput.readInt()

    // Read the actual string bytes
    val stringBytes = new Array[Byte](length)
    dataInput.readFully(stringBytes)

    // Convert the bytes back to a string using UTF-8 encoding
    new String(stringBytes, StandardCharsets.UTF_8)
  }

  /**
    * InputStream that is reading from a ByteBuffer.
    */
  class ByteBufferBackedInputStream(val buffer: ByteBuffer) extends InputStream {

    override def available: Int = buffer.remaining

    override def read: Int = {
      if (buffer.hasRemaining) {
        buffer.get & 0xFF
      } else {
        -1
      }
    }

    override def read(bytes: Array[Byte], off: Int, len: Int): Int = {
      if (!buffer.hasRemaining) {
        -1
      } else {
        val remainingLen = Math.min(len, buffer.remaining)
        buffer.get(bytes, off, remainingLen)
        remainingLen
      }
    }
  }
}
