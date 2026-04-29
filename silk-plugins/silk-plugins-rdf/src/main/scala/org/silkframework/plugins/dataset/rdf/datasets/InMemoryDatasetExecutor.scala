package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec, DatasetSpecAccess}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}

/**
 * Executor for [[InMemoryDataset]].
 *
 * In application-scoped mode (`workflowScoped == false`), wraps the dataset's static model.
 *
 * In workflow-scoped mode (`workflowScoped == true`), holds a separate Jena model for the
 * duration of a workflow execution. If the execution has a parent (nested workflow), the parent's
 * model for the same task is reused so that the nested workflow sees the data written by the parent.
 */
class InMemoryDatasetExecutor extends LocalDatasetExecutor[InMemoryDataset] {

  // Used only in workflow-scoped mode
  @volatile private var model: Model = _
  @volatile private var modelDataset: JenaModelDataset = _
  @volatile private var initialized: Boolean = false
  @volatile private var modelKey: Option[ExecutionModelKey] = None
  @volatile private var plugin: Option[InMemoryDataset] = None

  override def access(task: Task[DatasetSpec[InMemoryDataset]], execution: LocalExecution): DatasetAccess = {
    val datasetPlugin = task.data.plugin
    if (datasetPlugin.workflowScoped) {
      if (!initialized) {
        initialized = true
        model = execution.parentExecution
          .flatMap(datasetPlugin.findModel(_, task.id))
          .getOrElse(ModelFactory.createDefaultModel())
        modelDataset = JenaModelDataset.fromModel(model, dropGraphOnClear = false)
        val key = ExecutionModelKey(execution.executionId, task.id)
        datasetPlugin.registerModel(key, model)
        modelKey = Some(key)
        plugin  = Some(datasetPlugin)
      }
      datasetPlugin.updateData(model)
      DatasetSpecAccess(task.data, modelDataset)
    } else {
      val ds = JenaModelDataset.fromModel(datasetPlugin.model, dropGraphOnClear = datasetPlugin.clearGraphBeforeExecution)
      DatasetSpecAccess(task.data, ds)
    }
  }

  override def close(): Unit = {
    for {
      key <- modelKey
      p   <- plugin
    } p.removeModel(key)
  }
}
