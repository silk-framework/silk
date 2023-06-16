package org.silkframework.plugins.dataset.charset

import java.nio.charset._
import java.nio.charset.spi.CharsetProvider
import java.nio.{ByteBuffer, CharBuffer}
import java.util
import scala.jdk.CollectionConverters.IteratorHasAsJava


/**
  * A UTF8 charset that writes byte order marks (BOMs).
  */
object Utf8BomCharset extends Charset("UTF-8-BOM", Array.empty) {

  override def newDecoder: CharsetDecoder = StandardCharsets.UTF_8.newDecoder()

  override def newEncoder: CharsetEncoder = new Utf8BomEncoder(this)

  override def contains(cs: Charset): Boolean = {
    cs == this
  }
}

class Utf8BomCharsetProvider extends CharsetProvider {

  override def charsets(): util.Iterator[Charset] = {
    Iterator[Charset](Utf8BomCharset).asJava
  }

  override def charsetForName(charsetName: String): Charset = {
    if(charsetName == Utf8BomCharset.name) {
      Utf8BomCharset
    } else {
      null
    }
  }

}

class Utf8BomEncoder(charset: Charset) extends CharsetEncoder(charset, 1.1f, 3.0f) {

  private val encoder = StandardCharsets.UTF_8.newEncoder()

  private var doneBOM = false

  override def encodeLoop(in: CharBuffer, out: ByteBuffer): CoderResult = {
    if (!doneBOM && in.hasRemaining) {
      if (out.remaining < 3) {
        return CoderResult.OVERFLOW
      }
      out.put(0xEF.asInstanceOf[Byte])
      out.put(0xBB.asInstanceOf[Byte])
      out.put(0xBF.asInstanceOf[Byte])
      doneBOM = true
    }
    encoder.encode(in, out, false)
  }

  override protected def implReset(): Unit = {
    doneBOM = false
  }
}