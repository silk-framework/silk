package org.silkframework.util

import java.io.{BufferedOutputStream, OutputStream}

/**
  * A buffered output stream that closes the underlying stream on close.
  */
class SafeBufferedOutputStream(out: OutputStream, size: Int = SafeBufferedOutputStream.DefaultBufferSize) extends BufferedOutputStream(out, size) {

  override def close(): Unit = {
    try {
      out.flush()
    } finally {
      out.close()
    }
  }

}

object SafeBufferedOutputStream {

  final val DefaultBufferSize = 16384

}
