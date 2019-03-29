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

  private val log = Logger.getLogger(getClass.getName)

  private val zipFile: Resource = FileResource(file)

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
    BulkResourceSupport.combineStreams(inputStreams)
  }


  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @return Sequence of InputStream objects
    */
  def inputStreams: Seq[InputStream] = {
    subResources.map(_.inputStream)
  }

  /**
    * Returns a set of resources representing the files contained in a the bulk resource.
    * Each will have one unique input stream and the same meta data as the whole bulk resource.
    *
    * @return Sequence of resources
    */
  def subResources: Seq[WritableResource] = {
    try {
      val zipLoader = ZipResourceLoader(new ZipFile(path))
      zipLoader.list.sorted.map(f => ReadOnlyResource(zipLoader.get(f)))
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
  override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
    throw new UnsupportedOperationException("Writing to zip archives is not supported at the moment")
  }

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = try {
    val ftd = new File(zipFile.path)
    ftd.deleteOnExit()
    ftd.delete()
  } catch {
    case ex:IOException => log severe s"$zipFile could not be deleted:${ex.getMessage} "
  }

  override def read[T](reader: InputStream => T): T = super.read(reader)

}


/**
  * Companion with helper functions.
  */
object BulkResource {

  private final val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Returns true if the given resource is a BulkResource and false otherwise.
    * A BulkResource is detected if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource to check
    * @return true if an archive or folder
    */
  def isBulkResource(resource: Resource): Boolean = {
    resource.name.endsWith(".zip") && !new File(resource.path).isDirectory
  }

  /**
    * Returns a BulkResource depending on the given inputs location and name.
    * A BulkResource is returned if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource tha may be zip or folder
    * @return instance of BulkResource
    */
  def asBulkResource(resource: Resource): BulkResource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info s"Zip file Resource found: ${resource.name}"
      BulkResource(new File(resource.path))
    }
    else if (new File(resource.path).isDirectory) {
      log info s"Resource Folder found: ${resource.name}"
      throw new NotImplementedError("The bulk resource support does not work for non-zip files for now")    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

}