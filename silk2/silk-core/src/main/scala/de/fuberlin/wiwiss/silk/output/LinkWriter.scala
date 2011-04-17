package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}

/**
 * Represents an abstraction over an writer of links.
 *
 * Implementing classes of this trait must override the write method.
 */
trait LinkWriter extends Strategy
{
  /**
   * Initializes this writer.
   */
  def open() : Unit = {}

  /**
   * Writes a new link to this writer.
   */
  def write(link : Link, predicateUri : String) : Unit

  /**
   * Closes this writer.
   */
  def close() : Unit = {}
}

object LinkWriter extends Factory[LinkWriter]