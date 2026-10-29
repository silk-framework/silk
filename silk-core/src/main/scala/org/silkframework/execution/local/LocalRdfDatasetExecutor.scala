package org.silkframework.execution.local

import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDataset, RdfDatasetAccess, RdfDatasetSpecAccess}

/**
  * Base executor for RDF datasets.
  *
  * Subclasses build the execution-specific raw [[RdfDatasetAccess]] (the source/sinks and the SPARQL
  * endpoint) from the dataset task. This base wraps it with the DatasetSpec behaviour (URI attribute,
  * read-only check, safe-mode) via [[RdfDatasetSpecAccess]], so callers always go through the
  * execution-scoped access instead of the shared dataset plugin.
  */
abstract class LocalRdfDatasetExecutor[DatasetType <: RdfDataset] extends LocalDatasetExecutor[DatasetType] {

  /** Builds the execution-specific raw RDF access for the given dataset task. */
  protected def rdfAccess(task: Task[DatasetSpec[DatasetType]], execution: LocalExecution): RdfDatasetAccess

  override def access(task: Task[DatasetSpec[DatasetType]], execution: LocalExecution): DatasetAccess = {
    RdfDatasetSpecAccess(task.data, rdfAccess(task, execution))
  }
}
