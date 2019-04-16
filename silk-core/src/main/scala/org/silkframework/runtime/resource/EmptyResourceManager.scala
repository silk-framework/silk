package org.silkframework.runtime.resource
import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
 * A resource manager that does not provide any resources.
 */
case class EmptyResourceManager(name: String = "", parent: Option[ResourceManager] = None) extends ResourceManager {

  def basePath: String = parent.map(_.basePath).getOrElse("") + "/" + name

  override def list: Nil.type = Nil

  override def get(name: String, mustExist: Boolean): WritableResource = {
    if(mustExist) {
      throw new ResourceNotFoundException(s"Cannot retrieve resource $name as no resource manager is available.")
    } else {
      new NonExistentResource(name)
    }
  }

  override def delete(name: String) {
    throw new UnsupportedOperationException("Cannot delete resource.")
  }

  override def listChildren: Nil.type = Nil

  /**
    * Returns an empty resource manager.
    */
  override def child(name: String): ResourceManager = {
    EmptyResourceManager(name, Some(this))
  }

  /**
    * If the user calls get(mustExist = false), he gets a resource that actually does not exist.
    * We need this to be consistent with the semantics of the other resource managers.
    */
  private class NonExistentResource(val name: String) extends WritableResource {

    override def delete(): Unit = {}

    override def path: String = basePath + "/" + name

    override def exists: Boolean = false

    override def size: Option[Long] = None

    override def modificationTime: Option[Instant] = None

    override def inputStream: InputStream = {
      throw new ResourceNotFoundException(s"Cannot read from resource $path as no resource manager is available.")
    }

    override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
      throw new ResourceNotFoundException(s"Cannot write to resource $path as no resource manager is available.")
    }
  }
}
