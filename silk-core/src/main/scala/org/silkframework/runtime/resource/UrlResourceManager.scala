package org.silkframework.runtime.resource

import java.net.URL

/**
  * A resource manager that supports retrieving remote resources given by a URL.
  * If no URLs are provided it falls back to a specified local resource manager.
  */
case class UrlResourceManager(localResourceManager: ResourceManager) extends ResourceManager {

  override def get(name: String, mustExist: Boolean): WritableResource = {
    if(name.startsWith("http:") || name.startsWith("https:") || name.startsWith("ftp:")) {
      new ReadOnlyResource(UrlResource(new URL(name)))
    } else {
      localResourceManager.get(name, mustExist = mustExist)
    }
  }

  override def child(name: String): ResourceManager = UrlResourceManager(localResourceManager.child(name))

  override def parent: Option[ResourceManager] = for(parent <- localResourceManager.parent) yield UrlResourceManager(parent)

  override def delete(name: String): Unit = localResourceManager.delete(name)

  override def basePath: String = localResourceManager.basePath

  override def list: List[String] = localResourceManager.list

  override def listChildren: List[String] = localResourceManager.listChildren
}
