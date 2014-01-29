package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.InputStream

/**
 * A resource, such as a file, which is required by a plugin.
 */
trait Resource {

  /**
   * The local name of this resource.
   */
  def name: String

  /**
   * Loads the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def load: InputStream

  /**
   * Returns the name of this resource.
   */
  override def toString = name
}
