package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.{InputStream, OutputStream}

case class ReadOnlyResourceManager(loader: ResourceLoader) extends ResourceManager {

  override def get(name: String, mustExist: Boolean): WritableResource = new ReadOnlyResource(loader.get(name, mustExist))

  override def list = loader.list

  override def listChildren = loader.listChildren

  override def basePath: String = loader.basePath

  override def child(name: String): ResourceManager = new ReadOnlyResourceManager(loader.child(name))

  override def parent: Option[ResourceManager] = for(parent <- loader.parent) yield new ReadOnlyResourceManager(parent)

  override def delete(name: String) {
    throw new UnsupportedOperationException("ReadOnlyResourceManager does not support deleting resources.")
  }

  private class ReadOnlyResource(resource: Resource) extends WritableResource {

    override def name: String = resource.name

    override def path: String = resource.path

    override def load: InputStream = resource.load

    override def write(write: (OutputStream) => Unit): Unit = {
      throw new UnsupportedOperationException("ReadOnlyResourceManager does not support writing resources.")
    }
  }
}
