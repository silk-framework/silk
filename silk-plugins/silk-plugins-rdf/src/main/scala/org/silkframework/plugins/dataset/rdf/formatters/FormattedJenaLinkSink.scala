package org.silkframework.plugins.dataset.rdf.formatters

import org.apache.jena.rdf.model.Model
import org.silkframework.config.Prefixes
import org.silkframework.dataset.LinkSink
import org.silkframework.entity.Link
import org.silkframework.runtime.activity.UserContext

/**
 * A [[LinkSink]] implementation based on Jena's [[Model]] abstraction.
 */
class FormattedJenaLinkSink(model: Model,
                            formatter: RdfFormatter) extends LinkSink {
  /**
   * Initialize the link sink
   */
  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

  /**
   * Writes a new link to this writer.
   */
  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    this.synchronized {
      val linkModel = formatter.formatAsRDF(link, predicateUri, inversePredicateUri)
      model.add(linkModel)
    }
  }

  override def close()(implicit userContext: UserContext): Unit = {
    // Let the caller close this model
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    model.removeAll()
  }
}
