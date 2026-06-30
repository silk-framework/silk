package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}

/**
  * Executor for [[SparqlDataset]]. Builds the execution-scoped access over the dataset's SPARQL endpoint.
  */
class SparqlDatasetExecutor extends SparqlBackedDatasetExecutor[SparqlDataset] {

  override protected def params(plugin: SparqlDataset): SparqlParams = plugin.params

  override protected def endpoint(plugin: SparqlDataset): SparqlEndpoint = plugin.sparqlEndpoint

  override protected def dropGraphOnClear(plugin: SparqlDataset): Boolean = plugin.clearGraphBeforeExecution
}
