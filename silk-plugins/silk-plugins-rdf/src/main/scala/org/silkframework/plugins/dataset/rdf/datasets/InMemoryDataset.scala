package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.ModelFactory
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

@Plugin(
  id = InMemoryDataset.pluginId,
  label = "In-memory dataset",
  categories = Array(DatasetCategories.embedded),
  description = "A Dataset that holds all data in-memory.",
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
case class InMemoryDataset(@Param(label = "Clear graph before workflow execution (deprecated)",
                                  value = "This is deprecated, use the 'Clear dataset' operator instead to clear a dataset in a workflow. If set to true this will clear this dataset before it is used in a workflow execution.",
                                  advanced = true)
                           clearGraphBeforeExecution: Boolean = false) extends RdfDataset with TripleSinkDataset {

  private val model = ModelFactory.createDefaultModel()

  override val sparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(model)

  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source(implicit userContext: UserContext): DataSource = new SparqlSource(SparqlParams(), sparqlEndpoint)

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink(implicit userContext: UserContext): EntitySink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink(implicit userContext: UserContext): LinkSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)
}

object InMemoryDataset {
  final val pluginId = "inMemory"
}
