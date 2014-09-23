package de.fuberlin.wiwiss.silk.dataset.rdf

import de.fuberlin.wiwiss.silk.dataset.DatasetPlugin

trait RdfDatasetPlugin extends DatasetPlugin {

  def sparqlEndpoint: SparqlEndpoint

}
