package de.fuberlin.wiwiss.silk.datasource

import java.io.{FileInputStream, BufferedInputStream}

/**
 * A resource loader that loads resources from the classpath.
 */
class ClasspathResourceLoader(basePath: String) extends ResourceLoader {

  /**
   * Retrieves a resource by name.
   *
   * @param name The local name of the resource.
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   * @throws ResourceNotFoundException If no resource with the given name has been found.
   */
  override def load(name: String) = {
    val stream = getClass.getClassLoader.getResourceAsStream(basePath + "name")
    if(stream == null) throw new ResourceNotFoundException(s"Resource $name not found in classpath $basePath")
    stream
  }
}
