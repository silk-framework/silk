package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.InMemoryJenaModelEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.util.Identifier

import java.util.Collections

@Plugin(
  id = InMemoryDataset.pluginId,
  label = "In-memory Knowledge Graph",
  categories = Array(DatasetCategories.embedded),
  description = "A dataset that holds all data in-memory. " +
    "In the default (workflow-scoped) mode, data is isolated per workflow execution and shared with nested workflows that reference the same dataset task. " +
    "In application-scoped mode, data persists for the lifetime of the running process.",
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
         value = "If true (default), data is isolated per workflow execution and cleared after the execution ends, " +
                 "sharing data with nested workflows that reference the same dataset task. " +
                 "If false, data persists for the lifetime of the application process.")
  workflowScoped: Boolean = true,
  @Param(label = "Clear graph before workflow execution (deprecated)",
         value = "This is deprecated, use the 'Clear dataset' operator instead to clear a dataset in a workflow. If set to true this will clear this dataset before it is used in a workflow execution.",
         advanced = true)
  clearGraphBeforeExecution: Boolean = false
) extends RdfDataset with TripleSinkDataset {

  /**
   * The active endpoint backing this dataset. Owns its Jena model internally so the in-memory
   * size limit is tracked by a single counter regardless of how many times this dataset is accessed.
   *
   * Application-scoped mode: initialised once and never reassigned; holds data for the
   * lifetime of the process.
   *
   * Workflow-scoped mode: replaced by [[updateEndpoint]] each time [[InMemoryDatasetExecutor]]
   * activates a new execution.
   */
  @volatile private[datasets] var endpoint: InMemoryJenaModelEndpoint = new InMemoryJenaModelEndpoint()

  /**
   * Endpoints for all current workflow executions, keyed by [[ExecutionModelKey]].
   * Uses a WeakHashMap so entries are automatically cleaned up by GC when the key is no longer referenced.
   * Entries are also explicitly removed by [[InMemoryDatasetExecutor.close()]] when the execution finishes.
   */
  private val executionEndpoints: java.util.Map[ExecutionModelKey, InMemoryJenaModelEndpoint] =
    Collections.synchronizedMap(new java.util.WeakHashMap[ExecutionModelKey, InMemoryJenaModelEndpoint]())

  /**
   * Returns the endpoint registered under `key`, creating and registering a new one if absent.
   * Access is synchronized.
   */
  private[datasets] def getOrCreateEndpoint(key: ExecutionModelKey): InMemoryJenaModelEndpoint =
    executionEndpoints.synchronized {
      Option(executionEndpoints.get(key)).getOrElse {
        val newEndpoint = new InMemoryJenaModelEndpoint()
        executionEndpoints.put(key, newEndpoint)
        newEndpoint
      }
    }

  /** Looks up the endpoint anchored to the given execution's root for `taskId`, without creating one. */
  private[datasets] def findEndpoint(execution: LocalExecution, taskId: Identifier): Option[InMemoryJenaModelEndpoint] =
    Option(executionEndpoints.get(ExecutionModelKey(execution.rootExecution.executionId, taskId)))

  private[datasets] def removeEndpoint(key: ExecutionModelKey): Unit =
    executionEndpoints.remove(key)

  /** Switches [[endpoint]] to the given execution's endpoint so out-of-workflow reads see current data. */
  private[datasets] def updateEndpoint(newEndpoint: InMemoryJenaModelEndpoint): Unit =
    endpoint = newEndpoint

  // In workflow-scoped mode the executor owns the endpoint lifecycle, so sinks must not drop the graph.
  private def dropGraph: Boolean = !workflowScoped && clearGraphBeforeExecution

  override def sparqlEndpoint: SparqlEndpoint = endpoint

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
 * Key for the [[InMemoryDataset.executionEndpoints]] WeakHashMap (workflow-scoped mode).
 */
private[datasets] case class ExecutionModelKey(executionId: Identifier, taskId: Identifier)
