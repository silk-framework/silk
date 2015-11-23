package org.silkframework.plugins.dataset.rdf.formatters

import com.hp.hpl.jena.rdf.model.Model
import org.silkframework.entity.Link

/**
 * Writes more complex link structures as RDF.
 */
trait RdfFormatter {
  def format(link: Link, predicate: String): Model
}
