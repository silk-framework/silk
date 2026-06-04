package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec, DatasetSpecAccess}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
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
  @volatile private var modelDataset: JenaModelDataset = _
  @volatile private var initialized: Boolean = false
  @volatile private var modelKey: Option[ExecutionModelKey] = None
  @volatile private var plugin: Option[InMemoryDataset] = None
  @volatile private var ownsEndpoint: Boolean = false

  override def access(task: Task[DatasetSpec[InMemoryDataset]], execution: LocalExecution): DatasetAccess = {
    val datasetPlugin = task.data.plugin
    if (datasetPlugin.workflowScoped) {
      if (!initialized) {
        initialized = true
        // Anchor the endpoint to the root execution so the parent and all nested workflows
        // share one endpoint regardless of which of them accesses the dataset first.
        val key = ExecutionModelKey(execution.rootExecution.executionId, task.id)
        endpoint = datasetPlugin.getOrCreateEndpoint(key)
        modelDataset = JenaModelDataset.fromEndpoint(endpoint, dropGraphOnClear = false)
        modelKey = Some(key)
        plugin  = Some(datasetPlugin)
        // Only the top-level (root) execution disposes the shared endpoint
        ownsEndpoint = execution.parentExecution.isEmpty
      }
      datasetPlugin.updateEndpoint(endpoint)
      DatasetSpecAccess(task.data, modelDataset)
    } else {
      val ds = JenaModelDataset.fromEndpoint(datasetPlugin.endpoint, dropGraphOnClear = datasetPlugin.clearGraphBeforeExecution)
      DatasetSpecAccess(task.data, ds)
    }
  }

  override def close(): Unit = {
    if (ownsEndpoint) {
      for {
        key <- modelKey
        p   <- plugin
      } p.removeEndpoint(key)
    }
  }
}
