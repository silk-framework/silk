package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters

import com.hp.hpl.jena.rdf.model.Model
import de.fuberlin.wiwiss.silk.dataset.LinkSink
import de.fuberlin.wiwiss.silk.entity.Link

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
      val linkModel = formatter.format(link, predicateUri)
      model.add(linkModel)
    }
  }

  override def close(): Unit = {
    // Let the caller close this model
  }
}
