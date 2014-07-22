package de.fuberlin.wiwiss.silk.dataset

import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Represents an abstraction over an sink of data.
 *
 * Implementing classes of this trait must override the write methods.
 */
trait DataSink {
  /**
   * Initializes this writer.
   */
  def open() {}

  /**
   * Writes a new link to this writer.
   */
  def write(link: Link, predicateUri: String)

  def writeLiteralStatement(subject: String, predicate: String, value: String)

  def writeAll(links: Traversable[Link], predicateUri: String) {
    open()
    for (link <- links) write(link, predicateUri)
    close()
  }

  /**
   * Closes this writer.
   */
  def close() {}
}