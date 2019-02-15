package org.silkframework.plugins.dataset.rdf.access

import org.silkframework.dataset.rdf.SparqlEndpoint

/**
  * A sink that writes to a SPARQL endpoint.
  */
trait SparqlEndpointSink {
  def sparqlEndpoint: SparqlEndpoint
}
