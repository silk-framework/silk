package org.silkframework.plugins.dataset.rdf.formatters

import org.apache.jena.rdf.model.Model
import org.silkframework.entity.Link

/**
 * Writes more complex link structures as RDF.
 */
trait RdfFormatter {
  def formatAsRDF(link: Link, predicate: String): Model
}
