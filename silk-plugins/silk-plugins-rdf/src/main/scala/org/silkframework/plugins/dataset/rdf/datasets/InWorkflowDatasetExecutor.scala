package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.config.Task
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}

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
 */
class InWorkflowDatasetExecutor extends LocalDatasetExecutor[InWorkflowDataset] {

  private val model: Model = ModelFactory.createDefaultModel()

  // JenaModelDataset wraps the model and provides source/entitySink/linkSink backed by it.
  private val modelDataset: JenaModelDataset = JenaModelDataset(model)

  override def access(task: Task[DatasetSpec[InWorkflowDataset]], execution: LocalExecution): DatasetAccess = {
    task.data.plugin.updateData(model)
    modelDataset
  }

  override def close(): Unit = {}
}
