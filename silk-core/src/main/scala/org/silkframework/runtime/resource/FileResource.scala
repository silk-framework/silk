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

  override def load = {
    new BufferedInputStream(new FileInputStream(file))
  }

  override def write(write: (OutputStream) => Unit): Unit = {
    val baseDir = file.getParentFile
    if(!baseDir.exists && !baseDir.mkdirs())
      throw new IOException("Could not create directory at: " + baseDir.getCanonicalPath)
    val outputStream = new BufferedOutputStream(new FileOutputStream(file))
    write(outputStream)
    outputStream.close()
  }
}
