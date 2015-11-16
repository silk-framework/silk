package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters

import com.hp.hpl.jena.rdf.model.Model
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Writes more complex link structures as RDF.
 */
trait RdfFormatter {
  def format(link: Link, predicate: String): Model
}
