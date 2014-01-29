package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.InputStream

/**
 * A resource manager that does not provide any resources.
 */
class EmptyResourceManager extends ResourceManager {

  override def list = Nil

  override def get(name: String) = {
    throw new ResourceNotFoundException("Tried to retrieve a resource from an empty resource loader.")
  }
  
  override def put(name: String, inputStream: InputStream) {
    throw new UnsupportedOperationException("EmptyResourceManager does not support writing resources.")
  }

  override def delete(name: String) {
    throw new UnsupportedOperationException("EmptyResourceManager does not support deleting resources.")
  }
}
