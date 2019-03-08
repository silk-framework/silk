package org.silkframework.runtime.resource

import java.io._
import java.util.logging.Logger
import java.util.zip.ZipException

import org.apache.commons.io.input.ReaderInputStream

import scala.collection.JavaConverters

/**
  * Trait for Datasets that need to support zipped files with multiple resources.
  * Provides a checkResource function that returns a Resource or a BulkResource, depending on the input.
  * See @BulkResource.
  * To fully support zipped or partitioned resources the implementing class should use the methods of
  * BulkResource. It provides a input stream on the concatenated content of the zip or a set of input streams
  * that can be used in more complex methods of combining the input.
  */
trait BulkResourceSupport {

  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Returns a BulkResource depending on the given inputs location and name.
    * A BulkResource is returned if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource tha may be zip or folder
    * @return instance of BulkResource
    */
  def asBulkResource(resource: WritableResource): BulkResource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info "Zip file Resource found."
      BulkResource(new File(resource.path))
    }
    else if (new File(resource.path).isDirectory) {
      log info "Resource Folder found."
      BulkResource(new File(resource.path))
    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

  /**
    * Returns true if the given resource is a BulkResource and false otherwise.
    * A BulkResource is detected if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource to check
    * @return true if an archive or folder
    */
  def isBulkResource(resource: WritableResource): Boolean = {
    resource.name.endsWith(".zip") && !new File(resource.path).isDirectory
  }
}

object BulkResourceSupport {

  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @param bulkResource Zip or resource folder
    * @return Sequence of InputStream objects
    */
  def getInputStreamSet(bulkResource: BulkResource): Seq[InputStream] = {
    try {
      bulkResource.inputStreams
    }
    catch {
      case t: Throwable =>
        log severe s"Exception for zip resource ${bulkResource.path}: " + t.getMessage
        throw new ZipException(t.getMessage)
    }
  }

  /**
    * Returns the input streams belonging to the input resource. One for each file in the zipped bulk resource.
    *
    * @param bulkResource Input resource
    * @return Set of Streams
    */
  def getIndividualStreams(bulkResource: BulkResource): Seq[InputStream] = {
    bulkResource.inputStreams
  }


  /**
    * Returns one input stream belonging to the input resource. This input stream logically is equal to the input stream
    * on the concatenation of the individual resources in the bulk resource.
    *
    * If skipLines is non empty the concatenated input stream will skip the provided amount of lines in each file except
    * the first.
    *
    * @param bulkResource Input resource
    * @param skipLines Lines to skip at the beginning of each file except the 1st
    * @return
    */
  def getConcatenatedStream(bulkResource: BulkResource, skipLines: Option[Int] = None): InputStream = {
     combineStreams(bulkResource.inputStreams, skipLines)
  }

  /**
    * Combines a sequence of input streams (hopefully without crossing the streams) into one logical concatenation.
    *
    * @param streams Sequence of InputStream objects
    * @return InputStream
    */
  def combineStreams(streams: Seq[InputStream], skipLines: Option[Int] = None): InputStream = {
    if (skipLines.isEmpty) {
      val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream](streams.iterator)
      new SequenceInputStream(streamEnumeration.asJavaEnumeration)
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
  private def getSkipLinesInputStream(inputStream: InputStream, linesToSkip: Int = 1): InputStream = {
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
  def getNewlineInputStream: InputStream = {
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
        case _: Throwable => // who cares
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

}
