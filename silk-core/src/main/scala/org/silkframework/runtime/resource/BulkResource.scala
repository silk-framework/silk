package org.silkframework.runtime.resource

import java.io.{InputStream, SequenceInputStream}
import java.time.Instant
import java.util.logging.Logger
import java.util.zip.{ZipException, ZipFile}

import scala.collection.JavaConverters

object BulkResource extends Resource {

  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  var zipFile: WritableResource = _

  def apply(writableResource: WritableResource): Resource = {
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
    * the zip file. And only works if all files have the same schema.
    *
    * Warning: Only use when a literal concatenation is all you need.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream: InputStream = {
    combineStreams(unzippedStreams(zipFile.path))
  }

  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @param zipFilePath Location of the zip file
    * @return Sequence of InputStream objects
    */
  def unzippedStreams(zipFilePath: String): Seq[InputStream] = {
    try {
      val zipFile = new ZipFile(zipFilePath)
      val zipEntrySeq: Seq[InputStream] = for (entry <- zipFile.entries()) yield {
        zipFile.getInputStream(entry)
      }
      zipEntrySeq
    }
    catch {
      case t: Throwable =>
        log severe s"Exception for zip resource $zipFile: " + t.getMessage
        throw new ZipException(t.getMessage)
    }
  }

  /**
    * Combines (hopefully without crossing the streams) a sequence of input streams into one logical concatenation.
    *
    * @param streams Sequence of InputStream objects
    * @return InputStream
    */
  def combineStreams(streams: Seq[InputStream]): InputStream = {
    val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream]( streams.iterator )
    new SequenceInputStream(streamEnumeration.asJavaEnumeration)
  }

}
