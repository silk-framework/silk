package de.fuberlin.wiwiss.silk.dataset

import java.io.Closeable

import de.fuberlin.wiwiss.silk.entity.Link

/**
 * A sink that only writes entity links.
 */
trait LinkSink extends Closeable {
  /**
   * Initialize the link sink
   */
  def init(): Unit

  /**
   * Writes a new link to this writer.
   */
  def writeLink(link: Link, predicateUri: String): Unit

  /**
   * Writes a set of links.
   */
  def writeLinks(links: Traversable[Link], predicateUri: String): Unit = {
    init()
    for (link <- links) writeLink(link, predicateUri)
    close()
  }
}
