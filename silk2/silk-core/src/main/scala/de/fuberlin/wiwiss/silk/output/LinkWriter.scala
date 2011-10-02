package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Represents an abstraction over an writer of links.
 *
 * Implementing classes of this trait must override the write method.
 */
trait LinkWriter extends AnyPlugin {
  /**
   * Initializes this writer.
   */
  def open() {}

  /**
   * Writes a new link to this writer.
   */
  def write(link: Link, predicateUri: String)

  /**
   * Closes this writer.
   */
  def close() {}
}

object LinkWriter extends PluginFactory[LinkWriter]