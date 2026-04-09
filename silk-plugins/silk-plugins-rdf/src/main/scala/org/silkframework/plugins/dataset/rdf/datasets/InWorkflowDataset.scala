package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.ModelFactory
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = InWorkflowDataset.pluginId,
  label = "In-workflow dataset",
  categories = Array(DatasetCategories.embedded),
  description = "A Dataset that holds all data in-memory for the duration of a single workflow execution. " +
    "The data is stored separatly for each workflow execution. " +
    "The data is cleared once the workflow execution has finished.",
  relatedPlugins = Array(
    new PluginReference(
      id = InMemoryDataset.pluginId,
      description = "Both datasets hold data in-memory, but the in-memory dataset persists for the lifetime of the running process, " +
        "while the in-workflow dataset is scoped to a single workflow execution and cleared afterwards."
    )
  )
)
case class InWorkflowDataset() extends RdfDataset with TripleSinkDataset {

  // Empty placeholder model. Actual data is held in the executor (InWorkflowDatasetExecutor).
  // Framework code that bypasses access() and reads sparqlEndpoint directly will see empty results.
  private val emptyModel = ModelFactory.createDefaultModel()

  override val sparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(emptyModel)

  override def source(implicit userContext: UserContext): DataSource = new SparqlSource(SparqlParams(), sparqlEndpoint)

  override def entitySink(implicit userContext: UserContext): EntitySink = new SparqlSink(SparqlParams(), sparqlEndpoint)

  override def linkSink(implicit userContext: UserContext): LinkSink = new SparqlSink(SparqlParams(), sparqlEndpoint)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(SparqlParams(), sparqlEndpoint)
}

object InWorkflowDataset {
  final val pluginId = "inWorkflow"
}
