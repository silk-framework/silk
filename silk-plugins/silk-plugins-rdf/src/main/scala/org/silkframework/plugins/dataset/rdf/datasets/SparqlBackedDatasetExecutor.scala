package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.{RdfDataset, RdfDatasetAccess, SparqlEndpoint, SparqlParams}
import org.silkframework.execution.local.{LocalExecution, LocalRdfDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.access.SparqlDatasetAccess

/**
  * Base executor for RDF datasets whose access is a [[SparqlDatasetAccess]] over the plugin's SPARQL
  * endpoint. Subclasses provide only the values that differ — the endpoint and the clear behaviour, and
  * optionally the SPARQL params.
  */
abstract class SparqlBackedDatasetExecutor[DatasetType <: RdfDataset] extends LocalRdfDatasetExecutor[DatasetType] {

  /** SPARQL params for the access. Defaults to empty; override where the plugin carries its own. */
  protected def params(plugin: DatasetType): SparqlParams = SparqlParams()

  /** The execution-scoped SPARQL endpoint the access reads/writes through. */
  protected def endpoint(plugin: DatasetType): SparqlEndpoint

  /** Whether the sinks drop the graph on clear. */
  protected def dropGraphOnClear(plugin: DatasetType): Boolean

  override protected def rdfAccess(task: Task[DatasetSpec[DatasetType]], execution: LocalExecution): RdfDatasetAccess = {
    val plugin = task.data.plugin
    SparqlDatasetAccess(params(plugin), endpoint(plugin), dropGraphOnClear(plugin))
  }
}
