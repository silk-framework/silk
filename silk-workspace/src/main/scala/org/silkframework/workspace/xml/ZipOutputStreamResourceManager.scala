package org.silkframework.workspace.xml

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.collection.JavaConverters._

import org.silkframework.runtime.resource.{ResourceManager, WritableResource}

/**
  * A resource manager that writes all data to a ZIP output stream.
  * Does not support reading data or any modification to already written data.
  */
case class ZipOutputStreamResourceManager(zip: ZipOutputStream, basePath: String = "", closeEntriesAutomatically: Boolean = true) extends ResourceManager {

  private val children = new ConcurrentHashMap[String, ZipOutputStreamResourceManager]()
  private val resources = new ConcurrentHashMap[String, WritableResource]()

  override def child(name: String): ResourceManager = synchronized {
    val rm = ZipOutputStreamResourceManager(zip, basePath + "/" + name)
    children.put(name, rm)
    rm
  }

  override def get(name: String, mustExist: Boolean): WritableResource = synchronized {
    val res = ZipWritableResource(name, closeEntriesAutomatically)
    resources.put(name, res)
    res
  }

  override def parent: Option[ResourceManager] = throw ZipReadException()

  override def list: List[String] = resources.keys.asScala.toList

  override def listChildren: List[String] = children.keys.asScala.toList

  override def delete(name: String): Unit = throw ZipDeleteException()

  /**
    * Writes a single entry to the zip output stream.
    * @param name - the name of the entry
    * @param closeEntriesAutomatically - if true, each write call will create a new entry, else the caller has to manage entries
    */
  case class ZipWritableResource(name: String, closeEntriesAutomatically: Boolean) extends WritableResource {

    override def path: String = basePath.stripPrefix("/") + "/" + name

    override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
      if(closeEntriesAutomatically)
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
