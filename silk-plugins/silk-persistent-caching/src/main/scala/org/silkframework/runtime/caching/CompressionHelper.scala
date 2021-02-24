package org.silkframework.runtime.caching

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer

import net.jpountz.lz4.{LZ4BlockInputStream, LZ4BlockOutputStream, LZ4Factory}
import org.silkframework.util.StreamUtils

/**
  * Compression helper methods.
  */
object CompressionHelper {
  private val decompressor = LZ4Factory.fastestInstance().fastDecompressor()
  private val compressor = LZ4Factory.fastestInstance().fastCompressor()
  /** LZ4 compresses a byte array and returns the compressed byte array. */
  def lz4BlockCompress(byteArray: Array[Byte], addLengthPreamble: Boolean = false): Array[Byte] = {
    val byteArrayOS = new ByteArrayOutputStream()
    if(addLengthPreamble) {
      byteArrayOS.write(byteArray.length)
    }
    val compressedOS = new LZ4BlockOutputStream(byteArrayOS)
    compressedOS.write(byteArray)
    compressedOS.flush()
    compressedOS.close()
    byteArrayOS.toByteArray
  }

  /**
    * Compresses a byte array with lz4.
    * @param byteArray         The input byte array that should be compressed.
    * @param addLengthPreamble If the length of the input array should be stored as the first 4 bytes of the output array.
    *                          This is read by lz4decompressByteBuffer() if no length is specified.
    * @return
    */
  def lz4Compress(byteArray: Array[Byte], addLengthPreamble: Boolean): Array[Byte] = {
    val byteArrayOS = new ByteArrayOutputStream()
    if(addLengthPreamble) {
      val length = ByteBuffer.allocate(4).putInt(byteArray.length).array();
      byteArrayOS.write(length)
    }
    val compressedArray = compressor.compress(byteArray)
    byteArrayOS.write(compressedArray, 0, compressedArray.length)
    byteArrayOS.toByteArray
  }

  /** LZ4 block decompresses a LZ4 block compressed byte array.
    * Returns the decompressed byte array. */
  def lz4BlockDecompress(compressedByteArray: Array[Byte]): Array[Byte] = {
    val is = new LZ4BlockInputStream(new ByteArrayInputStream(compressedByteArray))
    val byteOS = new ByteArrayOutputStream()
    StreamUtils.fastStreamCopy(is, byteOS, close = false)
    byteOS.toByteArray
  }

  /** LZ4 block decompresses a LZ4 compressed ByteBuffer.
    * Returns the decompressed byte buffer.
    * @param length The length of the decompressed byte array. If not given, the length is expected to be the first
    *               4 bytes (int) of the byte buffer.
    **/
  def lz4decompressByteBuffer(byteBuffer: ByteBuffer, length: Option[Int] = None): ByteBuffer = {
    val decompressedSize = length match {
      case Some(l) => l
      case None => byteBuffer.getInt
    }
    val targetBuffer = ByteBuffer.allocateDirect(decompressedSize)
    decompressor.decompress(byteBuffer, byteBuffer.position(), targetBuffer, 0, decompressedSize)
    targetBuffer
  }
}
