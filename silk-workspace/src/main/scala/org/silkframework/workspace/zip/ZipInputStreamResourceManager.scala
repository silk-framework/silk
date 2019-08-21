package org.silkframework.workspace.zip

import java.io.{File, FileOutputStream}
import java.nio.file._
import java.util.concurrent.ConcurrentHashMap

import com.sun.nio.zipfs.ZipPath

import scala.collection.JavaConverters._
import org.silkframework.runtime.resource.{FileResource, ResourceManager, WritableResource}

/**
  * Resource manager for reading a zip file.
  * No write or delete operations are supported.
  * @param zip - the zip file to read
  * @param basePath - the root path
  */
class ZipInputStreamResourceManager(zip: Path, val basePath: String = "") extends ResourceManager {

  private def base = if(basePath.trim.isEmpty) "" else basePath.trim.stripSuffix("/").stripPrefix("/") + "/"
  private val children = new ConcurrentHashMap[Path, ZipInputStreamResourceManager]()
  private val resources = new ConcurrentHashMap[Path, WritableResource]()
  private val zipSystem: FileSystem = zip match{
    case zp: ZipPath => zp.getFileSystem
    case _ => FileSystems.newFileSystem(zip, null.asInstanceOf[ClassLoader])
  }

  override def child(name: String): ResourceManager = synchronized {
    val path = zipSystem.getPath(base + name)
    Option(children.get(path)) match{
      case Some(rm) => rm
      case None =>
        val rm = new ZipInputStreamResourceManager(path, path.toString)
        children.put(path, rm)
        rm
    }
  }

  override def get(name: String, mustExist: Boolean): WritableResource = synchronized {
    val zipPath = zipSystem.getPath(base + name)
    Option(resources.get(zipPath)) match{
      case Some(wr) => wr
      case None =>
        val index = if(name.lastIndexOf('.') < 0) name.length else name.lastIndexOf('.')
        val (prefix: String, suffix: String) = name.splitAt(index)
        val targetFile = File.createTempFile(basePath.stripSuffix("/") + "/" + prefix, suffix)
        Files.copy(zipPath, new FileOutputStream(targetFile))
        val res = FileResource(targetFile)
        resources.put(zipPath, res)
        res
    }
  }

  override def parent: Option[ResourceManager] = throw ZipReadException()

  override def list: List[String] = resources.keys.asScala.toList.map(_.toString)

  override def listChildren: List[String] = children.keys.asScala.toList.map(_.toString)

  override def delete(name: String): Unit = throw ZipDeleteException()

  override def close(): Unit = {
    zipSystem.close()
  }
}
