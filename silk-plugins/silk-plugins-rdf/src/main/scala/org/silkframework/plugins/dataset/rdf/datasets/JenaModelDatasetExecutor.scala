package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset.rdf.SparqlEndpoint

/**
  * Executor for [[JenaModelDataset]]. Builds the execution-scoped access over the dataset's
  * Jena model SPARQL endpoint.
  */
class JenaModelDatasetExecutor extends SparqlBackedDatasetExecutor[JenaModelDataset] {

  override protected def endpoint(plugin: JenaModelDataset): SparqlEndpoint = plugin.sparqlEndpoint

  override protected def dropGraphOnClear(plugin: JenaModelDataset): Boolean = plugin.dropGraphOnClear
}
