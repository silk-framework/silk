package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.InputStream
import scala.io.{Codec, Source}

/**
 * A resource, such as a file, which is required by a plugin.
 */
trait Resource {

  /**
   * The local name of this resource.
   */
  def name: String

  /**
   * The path of this resource.
   */
  def path: String

  /**
   * Loads the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def load: InputStream

  /**
   * Loads this resource into a string.
   */
  def loadAsString = {
    Source.fromInputStream(load)(Codec.UTF8).getLines.mkString("\n")
  }

  /**
   * Returns the name of this resource.
   */
  override def toString = name
}
