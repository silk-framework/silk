package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.rdf.SparqlEndpoint

/**
  * A sink that writes to a SPARQL endpoint.
  */
trait SparqlEndpointSink {
  def sparqlEndpoint: SparqlEndpoint
}
