package org.silkframework.runtime.resource

import java.io._
import java.time.Instant
import java.util.logging.Logger
import java.util.zip.{ZipException, ZipFile}

import BulkResource._
import org.apache.commons.io.input.ReaderInputStream

import scala.collection.JavaConverters

class BulkResource extends Resource {

  var zipFile: WritableResource = _
  var replacementInputStream: Option[InputStream] = None

  def apply(writableResource: WritableResource): BulkResource = {
    zipFile = writableResource
    this
  }

  def apply(writableResource: WritableResource, inputStreamReplacement: InputStream): BulkResource = {
    replacementInputStream = Some(inputStreamReplacement)
    zipFile = writableResource
    this
  }

  /**
    * The local name of this resource.
    */
  override def name: String = zipFile.name

  /**
    * The path of this resource.
    */
  override def path: String = zipFile.path

  /**
    * Checks if this resource exists.
    */
  override def exists: Boolean = zipFile.exists

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  override def size: Option[Long] = zipFile.size

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  override def modificationTime: Option[Instant] = zipFile.modificationTime

  /**
    * Creates an input stream for reading the resource.
    * This method creates one input stream from the input streams of the resources contained in
    * the zip file. Returns a concatenated input stream or the input stream that was given as a
    * replacement in the constructor or with the replaceStream method.
    *
    * Warning: Only use when a literal concatenation is all you need, e.g. nt-files, or a correct
    * input stream replacement was set.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream: InputStream = {
    if (replacementInputStream.nonEmpty) log.warning(s"Returning InputStream that concatenates all resources in $name. " +
      s"Use the methods of the BulkResource object to get and combine the resources individually")
    replacementInputStream.getOrElse(getCombinedInputStream)
  }


  /**
    * Replaces the inputStream of the resource. Used to create a valid resource object with an input stream
    * that can be manually combined from the set of input streams accessible with [[getInputStreamSet]].
    * Does not change any other metadata like size, date etc.
    *
    * @param inputStreamReplacement Replacement Input Stream
    */
  def replaceInputStream(inputStreamReplacement: InputStream): Unit = {
    replacementInputStream = Some(inputStreamReplacement)
  }


  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @return Sequence of InputStream objects
    */
  def getInputStreamSet: Seq[InputStream] = {
    try {
      val zipFile = new ZipFile(path)
      val zipEntrySeq: Seq[InputStream] = for (entry <- zipFile.entries()) yield {
        zipFile.getInputStream(entry)
      }
      zipEntrySeq
    }
    catch {
      case t: Throwable =>
        log severe s"Exception for zip resource $path: " + t.getMessage
        throw new ZipException(t.getMessage)
    }
  }

  /**
    * This method creates one input stream from the input streams of the resources contained in
    * the zip file. This is equivalent to an input stream for a concatenation of the resource contents.
    * WARNING: Only use when a literal concatenation is all you need.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  def getCombinedInputStream: InputStream = {
    combineStreams(getInputStreamSet)
  }

}


/**
  * Companion with helper methods for obtaining InputStreams from achieves and folders.
  * The BulkResource handling should generally be done in a dataset that implements the BulkResourceSupport trait.
  */
object BulkResource {

  val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

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
    * Creates an input stream for a given bulk resource that represents a concatenation of the contained
    * files where the first line of each file except the first is removed.
    *
    * @param bulkResource Zip or resource folder
    * @return Sequence of InputStream objects
    */
  def getHeaderlessInputStream(bulkResource: BulkResource): InputStream = {
    val streams = getInputStreamSet(bulkResource)
    val head = streams.head
    val tail = streams.tail
    val streamsWithoutHeaders: Seq[InputStream] = tail.map(is => {
      val lis = new LineNumberReader(new InputStreamReader(is))
      val line = lis.readLine()
      log warning s"Skipping line $line while combining input streams."
      new ReaderInputStream(lis)
    })
    combineStreams(Seq(head) ++ streamsWithoutHeaders)
  }


  /**
    * Checks if the given archive or folder contains files that akk share the same first line.,
    * e.g. csv files with the same header in each part.
    *
    * @param bulkResource Zip or resource folder
    * @return True if all files in the archive hace the same first line
    */
  def hasEqualHeaders(bulkResource: BulkResource): Boolean = {
    val streams = getInputStreamSet(bulkResource)
    val headers = streams.map(is => {
      val lis = new LineNumberReader(new InputStreamReader(is))
      lis.readLine()
    })
    headers.reduce((h1, h2) => h1.equals(h2))
  }


  def removeDuplicateFileHeaders(bulkResource: BulkResource): WritableResource = {
    BulkResource(bulkResource, getHeaderlessInputStream(bulkResource))
  }

  /**
    * Combines a sequence of input streams (hopefully without crossing the streams) into one logical concatenation.
    *
    * @param streams Sequence of InputStream objects
    * @return InputStream
    */
  def combineStreams(streams: Seq[InputStream]): InputStream = {
    val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream](streams.iterator)
    new SequenceInputStream(streamEnumeration.asJavaEnumeration)
  }


}