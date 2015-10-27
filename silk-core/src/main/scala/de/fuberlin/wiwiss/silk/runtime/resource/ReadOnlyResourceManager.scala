package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.OutputStream

case class ReadOnlyResourceManager(loader: ResourceLoader) extends ResourceManager {

  override def get(name: String, mustExist: Boolean) = loader.get(name, mustExist)

  override def list = loader.list

  override def listChildren = loader.listChildren

  override def basePath: String = loader.basePath

  override def child(name: String): ResourceManager = new ReadOnlyResourceManager(loader.child(name))

  override def parent: Option[ResourceManager] = for(parent <- loader.parent) yield new ReadOnlyResourceManager(parent)

  override def put(name: String)(write: (OutputStream) => Unit) {
    throw new UnsupportedOperationException("ReadOnlyResourceManager does not support writing resources.")
  }

  override def delete(name: String) {
    throw new UnsupportedOperationException("ReadOnlyResourceManager does not support deleting resources.")
  }
}
