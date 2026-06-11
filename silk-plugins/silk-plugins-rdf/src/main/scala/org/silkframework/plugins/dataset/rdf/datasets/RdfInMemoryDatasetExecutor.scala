package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.{RdfDatasetAccess, SparqlParams}
import org.silkframework.execution.local.{LocalExecution, LocalRdfDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.access.SparqlDatasetAccess

/**
  * Executor for [[RdfInMemoryDataset]]. Builds the execution-scoped access over the dataset's
  * in-memory Jena model endpoint.
  */
class RdfInMemoryDatasetExecutor extends LocalRdfDatasetExecutor[RdfInMemoryDataset] {

  override protected def rdfAccess(task: Task[DatasetSpec[RdfInMemoryDataset]], execution: LocalExecution): RdfDatasetAccess = {
    val plugin = task.data.plugin
    SparqlDatasetAccess(SparqlParams(), plugin.sparqlEndpoint, dropGraphOnClear = plugin.clearBeforeExecution)
  }
}
