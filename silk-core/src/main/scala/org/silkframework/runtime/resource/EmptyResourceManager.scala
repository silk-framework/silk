package org.silkframework.runtime.resource

/**
 * A resource manager that does not provide any resources.
 */
object EmptyResourceManager extends ResourceManager {

  override def basePath = ""

  override def list = Nil

  override def get(name: String, mustExist: Boolean) = {
    throw new ResourceNotFoundException("Tried to retrieve a resource from an empty resource loader.")
  }

  override def delete(name: String) {
    throw new UnsupportedOperationException("EmptyResourceManager does not support deleting resources.")
  }

  override def listChildren = Nil

  override def child(name: String): ResourceManager = {
    throw new ResourceNotFoundException("Tried to retrieve the child of an empty resource loader.")
  }

  override def parent: Option[ResourceManager] = {
    throw new ResourceNotFoundException("Tried to retrieve the parent from an empty resource loader.")
  }
}
