package org.silkframework.runtime.resource

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import java.time.Instant

/**
  * A resource on the file system.
  *
  * @param file The file
  */
case class FileResource(file: File) extends WritableResource {

  val name = file.getName

  val path = file.getAbsolutePath

  def exists = file.exists()

  def size = Some(file.length)

  def modificationTime = Some(Instant.ofEpochMilli(file.lastModified()))

  override def inputStream = {
    new BufferedInputStream(new FileInputStream(file))
  }

  /**
   * Lets the caller write into an [[OutputStream]] via the write function of the resource and closes it
   * after it returns.
   * @param write A function that accepts an output stream and writes to it.
   */
  override def write(append: Boolean = false)(write: (OutputStream) => Unit): Unit = {
    createDirectory()
    val outputStream = new BufferedOutputStream(new FileOutputStream(file, append))
    try {
      write(outputStream)
    } finally {
      outputStream.close()
    }
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
    val baseDir = file.getParentFile
    if(!baseDir.exists && !baseDir.mkdirs()) {
      throw new IOException("Could not create directory at: " + baseDir.getCanonicalPath)
    }
  }
}
