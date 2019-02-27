package org.silkframework.plugins.dataset.csv

import java.io._
import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipInputStream}

import org.silkframework.runtime.resource.{Resource, WritableResource}

import scala.collection.JavaConverters
trait BulkResourceSupport {

  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  def checkResource(resource: WritableResource): Resource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info "Zipped Resource found."
      BulkResource(resource)
    }
    else if (new File(resource.path).isDirectory) {
      log info "Resource Folder found."
      resource
    }
    else{
      resource
    }
  }

}

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
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream: InputStream = {
    // unpack
    val tempDir = new File(UUID.randomUUID().toString)
    tempDir.deleteOnExit()
    val individualFiles = tempDir.listFiles()
    // create is for each
    val inputStreams = for (file <- individualFiles) yield {
      new FileInputStream(file)
    }

    val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream]( inputStreams.iterator )
    new SequenceInputStream(streamEnumeration.asJavaEnumeration)
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

  /**
    * Create temporary folder.
    *
    * @param tmpName
    * @return
    */
  def createTempDir(tmpName: String): String = {
    val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
    val name: Path = tmpDir.getFileSystem.getPath(tmpName)
    if (name.getParent != null) throw new IllegalArgumentException("Invalid name for tmp directory")
    tmpDir.resolve(name).toString

  }
}
