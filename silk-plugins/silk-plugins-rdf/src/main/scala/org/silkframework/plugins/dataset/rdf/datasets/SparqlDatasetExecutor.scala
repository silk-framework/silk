package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.RdfDatasetAccess
import org.silkframework.execution.local.{LocalExecution, LocalRdfDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.access.SparqlDatasetAccess

/**
  * Executor for [[SparqlDataset]]. Builds the execution-scoped access over the dataset's SPARQL endpoint.
  */
class SparqlDatasetExecutor extends LocalRdfDatasetExecutor[SparqlDataset] {

  override protected def rdfAccess(task: Task[DatasetSpec[SparqlDataset]], execution: LocalExecution): RdfDatasetAccess = {
    val plugin = task.data.plugin
    SparqlDatasetAccess(plugin.params, plugin.sparqlEndpoint, dropGraphOnClear = plugin.clearGraphBeforeExecution)
  }
}
