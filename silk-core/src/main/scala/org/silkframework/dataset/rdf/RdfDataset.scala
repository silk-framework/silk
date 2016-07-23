package org.silkframework.dataset.rdf

import org.silkframework.dataset.Dataset

trait RdfDataset extends Dataset {

  def sparqlEndpoint: SparqlEndpoint

}
