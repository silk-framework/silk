package de.fuberlin.wiwiss.silk.datasource

import java.io.InputStream
import java.util.NoSuchElementException

/**
 * Loads external resources that are required by a data set, such as files.
 */
trait ResourceLoader {

  /**
   * Retrieves a name resource.
   *
   * @param name The name of the resource.
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   * @throws ResourceNotFoundException If no resource with the given name has been found.
   */
  def load(name: String): InputStream
}
