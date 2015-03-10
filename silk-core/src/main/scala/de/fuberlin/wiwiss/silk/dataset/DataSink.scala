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
   *
   * @param properties The list of properties of the entities to be written.
   */
  def open(properties: Seq[String] = Seq.empty) {}

  /**
   * Writes a new entity.
   *
   * @param subject The subject URI of the entity.
   * @param values The list of values of the entity. For each property that has been provided
   *               when opening this writer, it must contain a set of values.
   */
  def writeEntity(subject: String, values: Seq[Set[String]])

  /**
   * Writes a new link to this writer.
   */
  def writeLink(link: Link, predicateUri: String)

  /**
   * Writes a set of links.
   */
  def writeLinks(links: Traversable[Link], predicateUri: String) {
    open()
    for (link <- links) writeLink(link, predicateUri)
    close()
  }

  /**
   * Closes this writer.
   */
  def close() {}
}