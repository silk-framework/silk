package org.silkframework.runtime.resource.zip

import org.silkframework.runtime.resource.{ResourceManager, WritableResource}

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.{Deflater, ZipEntry, ZipOutputStream}
import scala.jdk.CollectionConverters.EnumerationHasAsScala


/**
  * A resource manager that writes all data to a ZIP output stream.
  * Does not support reading data or any modification to already written data.
  */
class ZipOutputStreamResourceManager(zip: OutputStream, val basePath: String = "", closeEntriesAutomatically: Boolean = true) extends ResourceManager {

  def this(zipFile: File, basePath: String, closeEntriesAutomatically: Boolean) = this(new FileOutputStream(zipFile), basePath, closeEntriesAutomatically)

  private var buffered: Option[BufferedOutputStream] = None
  private val zipOutput = if(zip.getClass == classOf[ZipOutputStream]){
    zip.asInstanceOf[ZipOutputStream]
  }
  else{
    buffered = Some(new BufferedOutputStream(zip))
    new ZipOutputStream(buffered.get)
  }

  private def base = if(basePath.trim.isEmpty) "" else basePath.trim.stripSuffix("/").stripPrefix("/") + "/"
  private val children = new ConcurrentHashMap[String, ZipOutputStreamResourceManager]()
  private val resources = new ConcurrentHashMap[String, WritableResource]()
  zipOutput.setLevel(Deflater.DEFAULT_COMPRESSION)

  /**
    * Use this to coordinate the setting of ZipEntries yourself (i.e. closeEntriesAutomatically == false)
    * @param entry - the next ZipEntry
    */
  def putNextZipEntry(entry: ZipEntry): Unit ={
    zipOutput.putNextEntry(entry)
  }

  override def child(name: String): ResourceManager = synchronized {
    val resolvedPath = resolvePath(name)
    val rm = new ZipOutputStreamResourceManager(zipOutput, resolvedPath, closeEntriesAutomatically)
    children.put(name, rm)
    rm
  }

  private def resolvePath(path: String): String = {
    if(!Paths.get(path).normalize.toString.startsWith("..")) {
      base + path
    } else{
      throw new IllegalArgumentException("The path requested would be outside the Zip resource.")
    }
  }

  override def get(name: String, mustExist: Boolean): WritableResource = synchronized {
    val resolvedPath = resolvePath(name)
    val res = ZipOutputStreamResource(name, resolvedPath, zipOutput, closeEntriesAutomatically)
    resources.put(name, res)
    res
  }

  override def parent: Option[ResourceManager] = throw ZipReadException()

  override def list: List[String] = resources.keys.asScala.toList

  override def listChildren: List[String] = children.keys.asScala.toList

  override def delete(name: String): Unit = throw ZipDeleteException()

  override def close(): Unit = {
    super.close()
    zipOutput.close()
    buffered.foreach(_.close())
    zip.close()
  }
}
