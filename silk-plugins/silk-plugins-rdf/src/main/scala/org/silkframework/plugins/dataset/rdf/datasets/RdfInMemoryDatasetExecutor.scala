package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset.rdf.SparqlEndpoint

/**
  * Executor for [[RdfInMemoryDataset]]. Builds the execution-scoped access over the dataset's
  * in-memory Jena model endpoint.
  */
class RdfInMemoryDatasetExecutor extends SparqlBackedDatasetExecutor[RdfInMemoryDataset] {

  override protected def endpoint(plugin: RdfInMemoryDataset): SparqlEndpoint = plugin.sparqlEndpoint

  override protected def dropGraphOnClear(plugin: RdfInMemoryDataset): Boolean = plugin.clearBeforeExecution
}
