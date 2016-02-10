package org.silkframework.dataset

import org.silkframework.entity.Link

/**
 * A sink that only writes entity links.
 */
trait LinkSink extends DataSink {
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
