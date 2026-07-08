package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDatasetAccess, RdfDatasetSpecAccess, SparqlParams}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.plugins.dataset.rdf.access.SparqlDatasetAccess
import org.silkframework.plugins.dataset.rdf.endpoint.InMemoryJenaModelEndpoint

/**
 * Executor for [[InMemoryDataset]].
 *
 * In application-scoped mode (`workflowScoped == false`), wraps the dataset's persistent endpoint.
 *
 * In workflow-scoped mode (`workflowScoped == true`), holds a separate [[InMemoryJenaModelEndpoint]]
 * for the duration of a workflow execution. The endpoint is anchored to the root execution of the
 * workflow tree, so a workflow and its nested workflows that reference the same dataset task all
 * share one endpoint regardless of which of them accesses the dataset first. The endpoint is
 * disposed only when the root execution finishes.
 */
class InMemoryDatasetExecutor extends LocalDatasetExecutor[InMemoryDataset] {

  // Used only in workflow-scoped mode
  @volatile private var endpoint: InMemoryJenaModelEndpoint = _
  @volatile private var modelAccess: RdfDatasetAccess = _
  @volatile private var initialized: Boolean = false
  @volatile private var closed: Boolean = false

  override def access(task: Task[DatasetSpec[InMemoryDataset]], execution: LocalExecution): DatasetAccess = {
    require(!closed, "Cannot access an InMemoryDatasetExecutor after it has been closed.")
    val datasetPlugin = task.data.plugin
    if (datasetPlugin.workflowScoped) {
      if (execution.rootExecution.workflowId.isEmpty) {
        // Out-of-workflow access (e.g. reading results after a workflow finished): reuse the dataset's
        // current endpoint, which still holds the most recent workflow execution's data, instead of
        // creating a new isolated, empty one. Don't touch this executor's execution-scoped state.
        RdfDatasetSpecAccess(task.data, SparqlDatasetAccess(SparqlParams(), datasetPlugin.endpoint, dropGraphOnClear = false))
      } else {
        if (!initialized) {
          initialized = true
          // Anchor the endpoint to the root execution so the parent and all nested workflows share one
          // endpoint regardless of which accesses first. The root execution owns its lifecycle.
          val key = ExecutionModelKey(execution.rootExecution.executionId, task.id)
          endpoint = datasetPlugin.getOrCreateEndpoint(key, execution.rootExecution)
          modelAccess = SparqlDatasetAccess(SparqlParams(), endpoint, dropGraphOnClear = false)
        }
        datasetPlugin.updateEndpoint(endpoint)
        RdfDatasetSpecAccess(task.data, modelAccess)
      }
    } else {
      val access = SparqlDatasetAccess(SparqlParams(), datasetPlugin.endpoint, dropGraphOnClear = datasetPlugin.clearGraphBeforeExecution)
      RdfDatasetSpecAccess(task.data, access)
    }
  }

  override def close(): Unit = {
    // The root execution owns the shared endpoint's lifecycle, so close() only drops this executor's references.
    endpoint = null
    modelAccess = null
    closed = true
  }
}
