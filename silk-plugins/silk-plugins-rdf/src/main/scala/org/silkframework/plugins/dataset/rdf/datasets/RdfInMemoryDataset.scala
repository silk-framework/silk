package org.silkframework.plugins.dataset.rdf.datasets

import java.io.StringReader

import org.apache.jena.rdf.model.ModelFactory
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{Param, Plugin}

/**
  * A Dataset where all entities are given directly in the configuration.
  *
  * Parameters:
  * - '''data''': The RDf data
  * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-Triples", "Turtle"
  */
@Plugin(id = "rdf", label = "RDF in-memory", description = "A Dataset where all entities are given directly in the configuration.")
case class RdfInMemoryDataset(data: String,
                              format: String,
                              @Param(label = "Clear graph before workflow execution",
                                value = "If set to true this will clear the specified graph before executing a workflow that writes to it.")
                              clearBeforeExecution: Boolean = true) extends RdfDataset with TripleSinkDataset {

  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(data), null, format)

  override val sparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(model)

  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source(implicit userContext: UserContext): DataSource = new SparqlSource(SparqlParams(), sparqlEndpoint)

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink(implicit userContext: UserContext): EntitySink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearBeforeExecution)

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink(implicit userContext: UserContext): LinkSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearBeforeExecution)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(SparqlParams(), sparqlEndpoint, dropGraphOnClear = clearBeforeExecution)
}
