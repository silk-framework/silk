package org.silkframework.runtime.resource

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import java.time.Instant
import org.silkframework.util.FileUtils._

/**
  * A resource on the file system.
  *
  * @param file The file
  */
case class FileResource(file: File)
    extends WritableResource
        with DeleteUnderlyingResourceOnGC {

  @volatile
  private var _deleteOnGC = false

  val name: String = file.getName

  val path: String = file.getAbsolutePath

  def exists: Boolean = file.exists()

  def size: Option[Long] = Some(file.length)

  def modificationTime: Option[Instant] = Some(Instant.ofEpochMilli(file.lastModified()))

  override def inputStream: BufferedInputStream = {
    new BufferedInputStream(new FileInputStream(file))
  }

  /**
    * Creates an output stream for writing to this resource.
    * The caller is responsible for closing the stream after writing.
    * Using [[write()]] is preferred as it takes care of closing the output stream.
    */
  override def createOutputStream(append: Boolean): OutputStream = {
    createDirectory()
    new BufferedOutputStream(new FileOutputStream(file, append))
  }

  override def deleteOnGC: Boolean = _deleteOnGC

  def setDeleteOnGC(value: Boolean): Unit = { _deleteOnGC = value }

  /**
    * Creates an empty file, overriding any existing and creating the required directories
    */
  def createEmpty(): Unit ={
    createDirectory()
    file.createNewFile()
  }

  /**
    * Writes a file.
    */
  override def writeFile(file: File): Unit = {
    createDirectory()
    Files.copy(file.toPath, this.file.toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = file.delete()

  private def createDirectory(): Unit = {
    Option(file.getParentFile).foreach(_.safeMkdirs())
  }
}
