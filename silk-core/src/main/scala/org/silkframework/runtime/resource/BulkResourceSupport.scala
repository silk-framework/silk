package org.silkframework.runtime.resource

import java.io._
import java.util.logging.Logger
import java.util.zip.{ZipException, ZipFile}

import org.apache.commons.io.input.ReaderInputStream
import org.silkframework.runtime.resource.BulkResource.log

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

  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @param bulkResource Zip or resource folder
    * @return Sequence of InputStream objects
    */
  def getInputStreamSet(bulkResource: BulkResource): Seq[InputStream] = {
    try {
      val zipFile = new ZipFile(bulkResource.path)
      val zipEntrySeq: Seq[InputStream] = for (entry <- zipFile.entries()) yield {
        zipFile.getInputStream(entry)
      }
      zipEntrySeq
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
        val streamsWithoutHeaders: Seq[InputStream] = tail.map(is => {
          val lis = new LineNumberReader(new InputStreamReader(is))
          for (i <- 0 to skipLines.get) {
            val line = lis.readLine()
            log warning s"Skipping line ${i +1} while combining input streams: \n $line"
            line
          }
          new ReaderInputStream(lis)
        })
        combineStreams(Seq(head) ++ streamsWithoutHeaders, None)
      }
      else {
        throw new IllegalArgumentException("The line to skip must be None or a number greater 0.")
      }
    }
  }

}
