package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}
import org.silkframework.util.Identifier

import java.util.Collections

@Plugin(
  id = InWorkflowDataset.pluginId,
  label = "In-workflow dataset",
  categories = Array(DatasetCategories.embedded),
  description = "A Dataset that holds all data in-memory, scoped to a single workflow execution. " +
    "The data is stored separately for each workflow execution. " +
    "A dataset in a nested workflow shares the same model as the parent, so data written by the parent is available in the nested workflow and vice versa.",
  documentationFile = "InWorkflowDataset.md",
  relatedPlugins = Array(
    new PluginReference(
      id = InMemoryDataset.pluginId,
      description = "Both datasets hold data in-memory, but the in-memory dataset persists for the lifetime of the running process, " +
        "while the in-workflow dataset is scoped to a single workflow execution."
    )
  )
)
case class InWorkflowDataset() extends RdfDataset with TripleSinkDataset {

  // Starts as an empty model so reads before any execution see empty (not null) results.
  // Replaced by a new JenaModelEndpoint when an executor registers its model via updateData.
  @volatile
  private var mostRecentSparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(ModelFactory.createDefaultModel())

  /**
   * Models for all current workflow executions, keyed by [[ExecutionModelKey]].
   * Uses a WeakHashMap so entries are automatically cleaned up by GC when the key is no longer referenced.
   * When the executor is GC'd, the entry is cleaned up.
   * Entries are also explicitly removed by [[InWorkflowDatasetExecutor.close()]] when the execution finishes.
   */
  private val executionModels: java.util.Map[ExecutionModelKey, Model] =
    Collections.synchronizedMap(new java.util.WeakHashMap[ExecutionModelKey, Model]())

  /** Registers the model for a given execution. */
  private[datasets] def registerModel(key: ExecutionModelKey, model: Model): Unit = {
    executionModels.put(key, model)
  }

  /**
   * Finds the model for the closest ancestor execution that has one registered for the given task.
   * Walks up the parentExecution chain, matching by both execution ID and task ID.
   */
  private[datasets] def findModel(execution: LocalExecution, taskId: Identifier): Option[Model] = {
    Option(executionModels.get(ExecutionModelKey(execution.executionId, taskId))).orElse(
      execution.parentExecution.flatMap(findModel(_, taskId))
    )
  }

  /** Removes the model for a given execution. Called by the executor on close(). */
  private[datasets] def removeModel(key: ExecutionModelKey): Unit = {
    executionModels.remove(key)
  }

  /**
   * Called by [[InWorkflowDatasetExecutor]] when a new execution starts.
   * Updates sparqlEndpoint so that direct reads see the latest executor's model.
   */
  private[datasets] def updateData(model: Model): Unit = {
    mostRecentSparqlEndpoint = new JenaModelEndpoint(model)
  }

  override def sparqlEndpoint: SparqlEndpoint = mostRecentSparqlEndpoint

  override def source(implicit userContext: UserContext): DataSource = new SparqlSource(SparqlParams(), sparqlEndpoint)

  override def entitySink(implicit userContext: UserContext): EntitySink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = false)

  override def linkSink(implicit userContext: UserContext): LinkSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = false)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = false)
}

object InWorkflowDataset {
  final val pluginId = "inWorkflow"
}

/**
 * Key for the [[InWorkflowDataset.executionModels]] WeakHashMap.
 */
private[datasets] case class ExecutionModelKey(executionId: Identifier, taskId: Identifier)
