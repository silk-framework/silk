package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import scala.xml.Elem

/**
 * An HTTP endpoint to issue SPARQL queries.
 */
trait HttpEndpoint {
  def select(url: String, login: Option[(String, String)]): Elem
  def update(url: String, body: String): Unit
}
