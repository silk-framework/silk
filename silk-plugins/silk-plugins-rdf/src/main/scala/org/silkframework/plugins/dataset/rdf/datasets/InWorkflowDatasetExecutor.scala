package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}

/**
 * Executor for [[InWorkflowDataset]].
 *
 * Holds the actual Jena model for the duration of a workflow execution.
 *
 * If the execution has a parent (nested workflow), the parent's model for the
 * same task is reused so that the nested workflow sees the data written by the parent.
 */
class InWorkflowDatasetExecutor extends LocalDatasetExecutor[InWorkflowDataset] {

  @volatile private var model: Model = _
  @volatile private var modelDataset: JenaModelDataset = _

  @volatile private var initialized: Boolean = false

  // Stored on first access for cleanup in close().
  // The executor is the only strong reference holder for the key, enabling WeakHashMap cleanup.
  @volatile private var modelKey: Option[ExecutionModelKey] = None
  @volatile private var plugin: Option[InWorkflowDataset] = None

  override def access(task: Task[DatasetSpec[InWorkflowDataset]], execution: LocalExecution): DatasetAccess = {
    if (!initialized) {
      println("INITIALIZED")
      initialized = true
      val datasetPlugin = task.data.plugin
      // Reuse the execution's model if available, otherwise create a new one.
      model = execution.parentExecution.flatMap(datasetPlugin.findModel(_, task.id)).getOrElse(ModelFactory.createDefaultModel())
      modelDataset = JenaModelDataset.fromModel(model, dropGraphOnClear = false)
      // Register this executor's model so nested workflows can find it.
      val key = ExecutionModelKey(execution.executionId, task.id)
      datasetPlugin.registerModel(key, model)
      modelKey = Some(key)
      plugin = Some(datasetPlugin)
    } else {
      println("FOUND: " + model)
    }
    task.data.plugin.updateData(model)
    modelDataset
  }

  override def close(): Unit = {
    for {
      key <- modelKey
      p <- plugin
    } {
      p.removeModel(key)
    }
  }
}
