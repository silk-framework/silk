package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.{RdfDatasetAccess, SparqlParams}
import org.silkframework.execution.local.{LocalExecution, LocalRdfDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.access.SparqlDatasetAccess

/**
  * Executor for [[JenaModelDataset]]. Builds the execution-scoped access over the dataset's
  * Jena model SPARQL endpoint.
  */
class JenaModelDatasetExecutor extends LocalRdfDatasetExecutor[JenaModelDataset] {

  override protected def rdfAccess(task: Task[DatasetSpec[JenaModelDataset]], execution: LocalExecution): RdfDatasetAccess = {
    val plugin = task.data.plugin
    SparqlDatasetAccess(SparqlParams(), plugin.sparqlEndpoint, dropGraphOnClear = plugin.dropGraphOnClear)
  }
}
