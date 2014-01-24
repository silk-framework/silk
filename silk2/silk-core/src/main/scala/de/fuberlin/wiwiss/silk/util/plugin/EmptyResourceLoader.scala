package de.fuberlin.wiwiss.silk.util.plugin

/**
 * A resource loader that does not provide any resources.
 */
class EmptyResourceLoader extends ResourceLoader {

  override def get(name: String) = {
    throw new ResourceNotFoundException("Tried to retrieve a resource from an empty resource loader.")
  }
}
