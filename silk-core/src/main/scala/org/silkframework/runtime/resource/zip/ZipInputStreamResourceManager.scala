package org.silkframework.runtime.resource.zip

import java.nio.file._
import java.util.concurrent.ConcurrentHashMap

import org.silkframework.runtime.resource._

/**
  * Resource manager for reading a zip file.
  * No write or delete operations are supported.
  * @param zip - the zip file to read
  * @param basePath - the root path
  */
class ZipInputStreamResourceManager(zipSystem: ZipResourceLoader, val basePath: String = "") extends ResourceManager {

  def this(resource: Resource, basePath: String) = this(ZipResourceLoader(resource, basePath), basePath)

  private def base = if(basePath.trim.isEmpty) "" else basePath.trim.stripSuffix("/").stripPrefix("/") + "/"
  private val children = new ConcurrentHashMap[Path, ZipInputStreamResourceManager]()
  private val resources = new ConcurrentHashMap[Path, WritableResource]()

  override def child(name: String): ResourceManager = synchronized {
    val path = zipSystem.child(name)
    new ZipInputStreamResourceManager(path.asInstanceOf[ZipResourceLoader], path.basePath)
  }

  override def get(name: String, mustExist: Boolean): WritableResource = synchronized {
    ReadOnlyResource(zipSystem.get(name, mustExist = true))
  }

  override def parent: Option[ResourceManager] = zipSystem.parent.map(rl => {
    val zipRl = rl.asInstanceOf[ZipResourceLoader]
    new ZipInputStreamResourceManager(zipRl, zipRl.basePath)
  })

  override def list: List[String] = zipSystem.list

  override def listChildren: List[String] = zipSystem.listChildren

  override def delete(name: String): Unit = throw ZipDeleteException()

}
