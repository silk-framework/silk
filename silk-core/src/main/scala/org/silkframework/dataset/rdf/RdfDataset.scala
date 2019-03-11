package org.silkframework.dataset.rdf

import java.io.InputStream

import org.silkframework.dataset.Dataset

trait RdfDataset extends Dataset {

  def sparqlEndpoint(sparqlInputStream: Option[InputStream] = None): SparqlEndpoint

}
