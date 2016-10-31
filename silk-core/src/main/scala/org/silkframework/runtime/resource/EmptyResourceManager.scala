package org.silkframework.runtime.resource

/**
 * A resource manager that does not provide any resources.
 */
object EmptyResourceManager extends ResourceManager {

  override def basePath = ""

  override def list = Nil

  override def get(name: String, mustExist: Boolean) = {
    throw new ResourceNotFoundException(s"Cannot retrieve resource $name as no resource manager is available.")
  }

  override def delete(name: String) {
    throw new UnsupportedOperationException("Cannot delete resource.")
  }

  override def listChildren = Nil

  /**
    * Returns an empty resource manager.
    */
  override def child(name: String): ResourceManager = {
    EmptyResourceManager
  }

  /**
    * The empty resource manager does not have a parent.
    *
    * @return None
    */
  override def parent: Option[ResourceManager] = {
    None
  }
}
