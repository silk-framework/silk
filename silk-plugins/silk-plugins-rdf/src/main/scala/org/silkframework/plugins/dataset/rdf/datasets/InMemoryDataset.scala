package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.util.Identifier

import java.util.Collections

@Plugin(
  id = InMemoryDataset.pluginId,
  label = "In-memory dataset",
  categories = Array(DatasetCategories.embedded),
  description = "A dataset that holds all data in-memory. " +
    "In the default (application-scoped) mode, data persists for the lifetime of the running process. " +
    "In workflow-scoped mode, data is isolated per workflow execution and shared with nested workflows that reference the same dataset task.",
  documentationFile = "InMemoryDataset.md",
  relatedPlugins = Array(
    new PluginReference(
      id = SparqlDataset.pluginId,
      description = "Data in the in-memory dataset does not persist beyond the running process. The SPARQL endpoint dataset connects to an external store that persists independently, which means switching between them changes not just where the data lives but whether it survives execution at all."
    ),
    new PluginReference(
      id = RdfFileDataset.pluginId,
      description = "Switching from the in-memory dataset to the RDF file dataset is not just adding persistence. The RDF file dataset loads the entire file into memory at read time and constrains output to N-Triples — neither of which the in-memory dataset does."
    )
  )
)
case class InMemoryDataset(
  @Param(label = "Workflow-scoped",
         value = "If true, data is isolated per workflow execution and cleared after the execution ends, " +
                 "sharing data with nested workflows that reference the same dataset task. " +
                 "If false (default), data persists for the lifetime of the application process.")
  workflowScoped: Boolean = false,
  @Param(label = "Clear graph before workflow execution (deprecated)",
         value = "This is deprecated, use the 'Clear dataset' operator instead to clear a dataset in a workflow. If set to true this will clear this dataset before it is used in a workflow execution.",
         advanced = true)
  clearGraphBeforeExecution: Boolean = false
) extends RdfDataset with TripleSinkDataset {

  /**
   * The active Jena model backing this dataset.
   *
   * Application-scoped mode: initialised once and never reassigned; holds data for the
   * lifetime of the process.
   *
   * Workflow-scoped mode: replaced by [[updateData]] each time [[InMemoryDatasetExecutor]]
   * activates a new execution.
   */
  @volatile private[datasets] var model: Model = ModelFactory.createDefaultModel()

  /**
   * Models for all current workflow executions, keyed by [[ExecutionModelKey]].
   * Uses a WeakHashMap so entries are automatically cleaned up by GC when the key is no longer referenced.
   * Entries are also explicitly removed by [[InMemoryDatasetExecutor.close()]] when the execution finishes.
   */
  private val executionModels: java.util.Map[ExecutionModelKey, Model] =
    Collections.synchronizedMap(new java.util.WeakHashMap[ExecutionModelKey, Model]())

  private[datasets] def registerModel(key: ExecutionModelKey, model: Model): Unit =
    executionModels.put(key, model)

  private[datasets] def findModel(execution: LocalExecution, taskId: Identifier): Option[Model] =
    Option(executionModels.get(ExecutionModelKey(execution.executionId, taskId))).orElse(
      execution.parentExecution.flatMap(findModel(_, taskId))
    )

  private[datasets] def removeModel(key: ExecutionModelKey): Unit =
    executionModels.remove(key)

  /** Switches [[model]] to the given execution's model so out-of-workflow reads see current data. */
  private[datasets] def updateData(newModel: Model): Unit =
    model = newModel

  // In workflow-scoped mode the executor owns the model lifecycle, so sinks must not drop the graph.
  private def dropGraph: Boolean = !workflowScoped && clearGraphBeforeExecution

  override def sparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(model)

  override def source(implicit userContext: UserContext): DataSource =
    new SparqlSource(SparqlParams(), sparqlEndpoint)

  override def entitySink(implicit userContext: UserContext): EntitySink =
    new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = dropGraph)

  override def linkSink(implicit userContext: UserContext): LinkSink =
    new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = dropGraph)

  override def tripleSink(implicit userContext: UserContext): TripleSink =
    new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = dropGraph)
}

object InMemoryDataset {
  final val pluginId = "inMemory"
}

/**
 * Key for the [[InMemoryDataset.executionModels]] WeakHashMap (workflow-scoped mode).
 */
private[datasets] case class ExecutionModelKey(executionId: Identifier, taskId: Identifier)
