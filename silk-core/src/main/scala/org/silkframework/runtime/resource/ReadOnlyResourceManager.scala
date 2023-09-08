package org.silkframework.runtime.resource

case class ReadOnlyResourceManager(loader: ResourceLoader) extends ResourceManager {

  override def get(name: String, mustExist: Boolean): WritableResource = new ReadOnlyResource(loader.get(name, mustExist))

  override def list: List[String] = loader.list

  override def listChildren: List[String] = loader.listChildren

  override def basePath: String = loader.basePath

  override def child(name: String): ResourceManager = ReadOnlyResourceManager(loader.child(name))

  override def parent: Option[ResourceManager] = for(parent <- loader.parent) yield ReadOnlyResourceManager(parent)

  override def delete(name: String): Unit = {
    throw new UnsupportedOperationException("ReadOnlyResourceManager does not support deleting resources.")
  }
}
