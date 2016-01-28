package org.silkframework.runtime.resource

import java.io._

/**
  * A resource on the file system.
  *
  * @param file The file
  */
class FileResource(val file: File) extends WritableResource {

  val name = file.getName

  val path = file.getAbsolutePath

  def exists = file.exists()

  override def load = {
    new BufferedInputStream(new FileInputStream(file))
  }

  /**
   * Lets the caller write into an [[OutputStream]] via the write function of the resource and closes it
   * after it returns.
   * @param write A function that accepts an output stream and writes to it.
   */
  override def write(write: (OutputStream) => Unit): Unit = {
    val baseDir = file.getParentFile
    if(!baseDir.exists && !baseDir.mkdirs())
      throw new IOException("Could not create directory at: " + baseDir.getCanonicalPath)
    val outputStream = new BufferedOutputStream(new FileOutputStream(file))
    write(outputStream)
    outputStream.flush()
    outputStream.close()
  }
}
