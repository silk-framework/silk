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
 * for the duration of a workflow execution. If the execution has a parent (nested workflow), the
 * parent's endpoint for the same task is reused so that the nested workflow sees the data written
 * by the parent.
 */
class InMemoryDatasetExecutor extends LocalDatasetExecutor[InMemoryDataset] {

  // Used only in workflow-scoped mode
  @volatile private var endpoint: InMemoryJenaModelEndpoint = _
  @volatile private var modelDataset: JenaModelDataset = _
  @volatile private var initialized: Boolean = false
  @volatile private var modelKey: Option[ExecutionModelKey] = None
  @volatile private var plugin: Option[InMemoryDataset] = None

  override def access(task: Task[DatasetSpec[InMemoryDataset]], execution: LocalExecution): DatasetAccess = {
    val datasetPlugin = task.data.plugin
    if (datasetPlugin.workflowScoped) {
      if (!initialized) {
        initialized = true
        endpoint = execution.parentExecution
          .flatMap(datasetPlugin.findEndpoint(_, task.id))
          .getOrElse(new InMemoryJenaModelEndpoint())
        modelDataset = JenaModelDataset.fromEndpoint(endpoint, dropGraphOnClear = false)
        val key = ExecutionModelKey(execution.executionId, task.id)
        datasetPlugin.registerEndpoint(key, endpoint)
        modelKey = Some(key)
        plugin  = Some(datasetPlugin)
      }
      datasetPlugin.updateEndpoint(endpoint)
      DatasetSpecAccess(task.data, modelDataset)
    } else {
      val ds = JenaModelDataset.fromEndpoint(datasetPlugin.endpoint, dropGraphOnClear = datasetPlugin.clearGraphBeforeExecution)
      DatasetSpecAccess(task.data, ds)
    }
  }

  override def close(): Unit = {
    for {
      key <- modelKey
      p   <- plugin
    } p.removeEndpoint(key)
  }
}
