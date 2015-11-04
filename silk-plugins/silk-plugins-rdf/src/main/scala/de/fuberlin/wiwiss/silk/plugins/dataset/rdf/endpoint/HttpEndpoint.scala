package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import scala.xml.Elem

/**
 * An HTTP endpoint to issue SPARQL queries.
 */
trait HttpEndpoint {
  def select(query: String): Elem
  def update(query: String): Unit
}
