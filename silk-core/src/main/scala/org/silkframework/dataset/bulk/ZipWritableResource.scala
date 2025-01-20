package org.silkframework.dataset.bulk

import org.silkframework.runtime.resource.WritableResource

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

/**
 * Resource that reads or writes the contents of a zip file.
 * Writing will put a single file into the zip.
 */
case class ZipWritableResource(resource: WritableResource) extends WritableResource {

  override def createOutputStream(append: Boolean): OutputStream = {
    if(append) {
      throw new IllegalArgumentException("Zip files cannot be appended")
    } else {
      val zip = new ZipOutputStream(resource.createOutputStream(append))
      zip.putNextEntry(new ZipEntry(resource.name.stripSuffix(".zip")))
      zip
    }
  }

  override def delete(): Unit = {
    resource.delete()
  }

  override def inputStream: InputStream = {
    val zipInputStream = new ZipInputStream(resource.inputStream)
    zipInputStream.getNextEntry
    zipInputStream
  }

  override def name: String = resource.name
  override def path: String = resource.path
  override def exists: Boolean = resource.exists
  override def size: Option[Long] = resource.size
  override def modificationTime: Option[Instant] = resource.modificationTime
}
