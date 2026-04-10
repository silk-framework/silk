package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.util.Identifier

/**
 * Executor for [[InWorkflowDataset]].
 *
 * Holds the actual Jena model for the duration of a workflow execution.
 * Overrides access() to expose the executor-owned model to the framework,
 * ensuring each workflow execution has its own isolated data.
 * When access() is called, the dataset's sparqlEndpoint is updated to point
 * to this executor's model so that framework code reading sparqlEndpoint
 * directly sees the data from the most recently started execution.
 * The model is retained after the execution ends so that the data remains
 * accessible via sparqlEndpoint until a new execution overwrites it.
 *
 * If the execution has a parent (nested workflow), the parent's model data
 * is copied into this executor's model on first access, so that the nested
 * workflow sees the data written by the parent.
 */
class InWorkflowDatasetExecutor extends LocalDatasetExecutor[InWorkflowDataset] {

  private var model: Model = _
  private var modelDataset: JenaModelDataset = _

  @volatile private var initialized: Boolean = false

  // Stored on first access for cleanup in close().
  private var executionId: Option[Identifier] = None
  private var plugin: Option[InWorkflowDataset] = None

  override def access(task: Task[DatasetSpec[InWorkflowDataset]], execution: LocalExecution): DatasetAccess = {
    if (!initialized) {
      initialized = true
      val datasetPlugin = task.data.plugin
      // Reuse the parent execution's model if available, otherwise create a new one.
      model = execution.parentExecution.flatMap(datasetPlugin.findModel).getOrElse(ModelFactory.createDefaultModel())
      modelDataset = JenaModelDataset(model)
      // Register this executor's model so nested workflows can find it.
      datasetPlugin.registerModel(execution.executionId, model)
      executionId = Some(execution.executionId)
      plugin = Some(datasetPlugin)
    }
    task.data.plugin.updateData(model)
    modelDataset
  }

  override def close(): Unit = {
    for {
      eid <- executionId
      p <- plugin
    } {
      p.removeModel(eid)
    }
  }
}
