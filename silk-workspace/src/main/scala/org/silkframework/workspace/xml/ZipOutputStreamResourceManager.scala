package org.silkframework.workspace.xml

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.silkframework.runtime.resource.{ResourceManager, WritableResource}

/**
  * A resource manager that writes all data to a ZIP output stream.
  * Does not support reading data or any modification to already written data.
  */
case class ZipOutputStreamResourceManager(zip: ZipOutputStream, basePath: String = "") extends ResourceManager {

  override def child(name: String): ResourceManager = ZipOutputStreamResourceManager(zip, basePath + "/" + name)

  override def get(name: String, mustExist: Boolean): WritableResource = {
    ZipWritableResource(name)
  }

  override def parent: Option[ResourceManager] = throw ZipReadException()

  override def list: List[String] = throw ZipReadException()

  override def listChildren: List[String] = throw ZipReadException()

  override def delete(name: String): Unit = throw ZipDeleteException()

  /**
    * Writes a single entry to the zip output stream.
    */
  case class ZipWritableResource(name: String) extends WritableResource {

    override def path: String = basePath.stripPrefix("/") + "/" + name

    override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
      zip.putNextEntry(new ZipEntry(path))
      write(zip)
    }

    override def exists: Boolean = false

    override def size: Option[Long] = None

    override def modificationTime: Option[Instant] = None

    override def delete(): Unit = throw ZipDeleteException()

    override def inputStream: InputStream = throw ZipReadException()
  }

  case class ZipReadException() extends UnsupportedOperationException(s"${classOf[ZipOutputStreamResourceManager]} does not support any read operations.")

  case class ZipDeleteException() extends UnsupportedOperationException(s"${classOf[ZipOutputStreamResourceManager]} does not support deleting resources.")
}
