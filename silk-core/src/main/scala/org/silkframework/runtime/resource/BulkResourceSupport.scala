package org.silkframework.runtime.resource

import java.io._
import java.util.logging.Logger

import org.apache.commons.io.input.ReaderInputStream

import scala.collection.JavaConverters

/**
  * Helper functions for data sets that need to support zipped files with multiple resources or similar structures.
  * Provides a checkResource function that checks and returns a Resource or a BulkResource, depending on the input.
  * See @BulkResource.
  *
  * The methods for creating determining individual distinct schemata inside the bulk resource and the methods
  * to create BulkResource objects have to be provided by the implementing class.
  *
  * This should be only used if the data set can quickly implement the logic needed, e.g. when multi schema resources are
  * not supported or support is possible on stream or data frame level. This is often the case when the input streams
  * can be combined to one logical stream or a union operation is trivial. Helper methods for that are provided by the
  * companion object.
  *
  * Alternatively, BulkResourceBasedDataSet can be used. This replaces not the dataset resource but rather it s data
  * source object.
  *
  * @see BulkResourceBasedDataSet
  * @see BulkResource
  */
object BulkResourceSupport {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * Combines a sequence of input streams (hopefully without crossing the streams) into one logical concatenation.
    *
    * @param streams Sequence of InputStream objects
    * @return InputStream
    */
  def combineStreams(streams: Seq[InputStream], skipLines: Option[Int] = None): InputStream = {
    if (skipLines.isEmpty) {
      val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream](streams.iterator)
      val combi = new SequenceInputStream(streamEnumeration.asJavaEnumeration)
      val copy = copyStream(combi)
      copy.reset()
      copy
    }
    else {
      if (skipLines.get > 0) {
        val head = streams.head
        val tail = streams.tail
        val newTail: Seq[InputStream] = tail.map(is => {
          val correctedStream = getSkipLinesInputStream(is, skipLines.get)
          new ReaderInputStream(new InputStreamReader(correctedStream))
        })
        combineStreams(Seq(head) ++ newTail, None)
      }
      else {
        throw new IllegalArgumentException("The line to skip must be None or a number greater 0.")
      }
    }
  }

  /**
    * Return an input stream that is equa to the given stream with the given
    * number of lines skipped.
    *
    * @param inputStream - input stream wirh lines to skip
    * @param linesToSkip - number of lines to skip
    * @return
    */
  private def getSkipLinesInputStream(inputStream: InputStream, linesToSkip: Int = 0): InputStream = {
    val lis = new LineNumberReader(new InputStreamReader(inputStream))
    for (i <-0 until linesToSkip) {
      val line = lis.readLine()
      log info s"Skipping line: $line in bulk resource."
    }
    combineStreams(Seq(getNewlineInputStream, new ReaderInputStream(lis)))
  }


  /**
    * Get an InputStream representing only a newline char.
    *
    * @return InputStream
    */
  private def getNewlineInputStream: InputStream = {
    val newline = System.lineSeparator()
    new ByteArrayInputStream(newline.getBytes())
  }

  /**
    * Closes the given set of streams. Calls wait() before closing if that is possible.
    *
    * @param streams Sequence of InoutStreams
    */
  def closeStreamSet(streams: Seq[InputStream]): Unit = {
    if (streams.nonEmpty) streams.foreach( s => {
      try s.wait()
      catch {
        case t: Throwable =>
          log severe "Some kind of exception occured when closing an input stream: " + t.getMessage
      }
      finally s.close()
    })
  }


  /**
    * Closes the given niput stream. Calls wait() before closing if that is possible.
    *
    * @param stream Input stream
    */
  def closeStream(stream: InputStream): Unit = {
    if (stream != null) {
      try stream.wait()
      catch {
        case _: Throwable => // who cares
      }
      finally stream.close()
    }
  }

  /**
    * Copy an input stream.
    *
    * @param inputStream stream to copy
    * @return
    */
  def copyStream(inputStream: InputStream): InputStream = {
  //  val text = scala.io.Source.fromInputStream(inputStream).mkString
    val stream: InputStream = new ByteArrayInputStream(streamToBytes(inputStream))
    inputStream.close()
    stream.reset()
    stream
  }


  /**
    * Creates a byte array from a stream.
    *
    * @param inputStream stream to get bytes from
    * @return
    */
  def streamToBytes(inputStream: InputStream): Array[Byte] = {
    val len = 16384
    val buf = Array.ofDim[Byte](len)
    val out = new ByteArrayOutputStream

    @scala.annotation.tailrec
    def copy(): Array[Byte] = {
      val n = inputStream.read(buf, 0, len)
      if (n != -1) {
        out.write(buf, 0, n); copy()
      }
      else {
        out.toByteArray
      }
    }

    copy()
  }

  /**
    * Print stream contents for log/test purposes
    * @param inputStream stream to print
    */
  def printStream(inputStream: InputStream): Unit = {
    val copy = copyStream(inputStream)
    var txt = ""
    var b = 0
    b = copy.read()
    while (b > 0) {
      txt += b.toChar
      b = copy.read()

    }
    println(txt.replaceAll("\r",""))
  }

}
