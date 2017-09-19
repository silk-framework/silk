package org.silkframework.dataset.rdf

/**
  * A plugin parameter that chooses a SPARQL enabled dataset from the available datasets.
  */
case class SparqlEndpointDatasetParameter(sparqlEnabledDataset: String) {
  override def toString: String = sparqlEnabledDataset
}
