package org.silkframework.dataset.rdf

import org.silkframework.dataset.DatasetPlugin

trait RdfDatasetPlugin extends DatasetPlugin {

  def sparqlEndpoint: SparqlEndpoint

}
