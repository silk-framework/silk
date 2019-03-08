package org.silkframework.runtime.resource

import java.io._
import java.time.Instant
import java.util.logging.Logger
import java.util.zip.{ZipEntry, ZipException, ZipFile}

import scala.collection.mutable

/**
  * Resource that represents an archive with multiple files.
  * Provides a singular Inputstream across the file contents ans otherwise uses
  * the meta data of the archieve.
  *
  * Mainly features a replaceable input stream and functions to get one
  * concatenated or a set of streams.
  *
  * @param file Zip resources
  */
case class BulkResource(file: File) extends WritableResource {

  val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  var zipFile: Resource = FileResource(file)
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
    if (replacementInputStream.nonEmpty) log.warning(s"Returning a stream that concatenates all resources in " +
      s"$name. Use the methods of the BulkResource object to get and combine the resources individually")
    replacementInputStream.getOrElse(BulkResourceSupport.combineStreams(inputStreams))
  }


  /**
    * Replaces the inputStream of the resource. Used to create a valid resource object with an input stream
    * that can be manually combined from the set of input streams accessible with [[inputStreams]].
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
  def inputStreams: Seq[InputStream] = {
    try {
      val zipFile = new ZipFile(path)
      val entries = zipFile.entries()
      val streams = new mutable.HashSet[ZipEntry]
      while (entries.hasMoreElements) streams.add(entries.nextElement())
      streams.map(s => zipFile.getInputStream(s)).toSeq
    }
    catch {
      case t: Throwable =>
        log severe s"Exception for zip resource $path: " + t.getMessage
        throw new ZipException(t.getMessage)
    }
  }


  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  override def write(append: Boolean)(write: OutputStream => Unit): Unit = ???

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = try {
    val ftd = new File(zipFile.path)
    ftd.deleteOnExit()
    ftd.delete()
  }
  catch {
    case ex:IOException => log severe s"$zipFile could not be deleted:${ex.getMessage} "
  }
}
//
//
///**
//  * Companion with helper methods for obtaining InputStreams from achieves and folders.
//  * The BulkResource handling should generally be done in a dataset that implements the BulkResourceSupport trait.
//  */
//object BulkResource {
//
//
//}