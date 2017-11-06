package org.silkframework.plugins.dataset.rdf.formatters

import org.apache.jena.rdf.model.Model
import org.silkframework.dataset.LinkSink
import org.silkframework.entity.Link

/**
 * A [[LinkSink]] implementation based on Jena's [[Model]] abstraction.
 */
class FormattedJenaLinkSink(model: Model,
                            formatter: RdfFormatter) extends LinkSink {
  /**
   * Initialize the link sink
   */
  override def init(): Unit = {}

  /**
   * Writes a new link to this writer.
   */
  override def writeLink(link: Link, predicateUri: String): Unit = {
    this.synchronized {
      val linkModel = formatter.formatAsRDF(link, predicateUri)
      model.add(linkModel)
    }
  }

  override def close(): Unit = {
    // Let the caller close this model
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(): Unit = {
    model.removeAll()
  }
}
