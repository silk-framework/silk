package org.silkframework.runtime.resource
import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
 * A resource manager that does not provide any resources.
 */
object EmptyResourceManager {

  private lazy val rootInstance = new EmptyResourceManagerImpl("", None)

  /** Retrieves the empty resource manager */
  def apply(): ResourceManager = rootInstance

  private case class EmptyResourceManagerImpl(name: String, parent: Option[ResourceManager]) extends ResourceManager {

    def basePath: String = parent.map(_.basePath).getOrElse("") + "/" + name

    override def list: Nil.type = Nil

    override def get(name: String, mustExist: Boolean): WritableResource = {
      if(mustExist) {
        throw new ResourceNotFoundException(s"Cannot retrieve resource $name as no resource manager is available.")
      } else {
        new NonExistentResource(basePath, name)
      }
    }

    override def delete(name: String): Unit = {
      throw new UnsupportedOperationException("Cannot delete resource.")
    }

    override def listChildren: Nil.type = Nil

    /**
      * Returns an empty resource manager.
      */
    override def child(name: String): ResourceManager = {
      new EmptyResourceManagerImpl(name, Some(this))
    }
  }

  /**
    * If the user calls get(mustExist = false), he gets a resource that actually does not exist.
    * We need this to be consistent with the semantics of the other resource managers.
    */
  private class NonExistentResource(val basePath: String, val name: String) extends WritableResource {

    override def delete(): Unit = {}

    override def path: String = basePath + "/" + name

    override def exists: Boolean = false

    override def size: Option[Long] = None

    override def modificationTime: Option[Instant] = None

    override def inputStream: InputStream = {
      throw new ResourceNotFoundException(s"Cannot read from resource $path as no resource manager is available.")
    }

    override def createOutputStream(append: Boolean = false): OutputStream = {
      throw new ResourceNotFoundException(s"Cannot write to resource $path as no resource manager is available.")
    }
  }

}
