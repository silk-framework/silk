package de.fuberlin.wiwiss.silk.datasource

import java.io.{FileInputStream, BufferedInputStream}

/**
 * A resource loader that does provide any resources.
 */
class EmptyResourceLoader extends ResourceLoader {

  override def load(name: String) = {
    throw new ResourceNotFoundException("Tried to retrieve a resource from an empty resource loader.")
  }
}
