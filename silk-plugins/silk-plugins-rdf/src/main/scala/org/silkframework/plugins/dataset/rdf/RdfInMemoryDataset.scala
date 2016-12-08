package org.silkframework.plugins.dataset.rdf

import java.io.StringReader

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.silkframework.dataset.rdf.{ClearableDatasetGraphTrait, RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.dataset._
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.runtime.plugin.{Param, Plugin}

/**
  * A Dataset where all entities are given directly in the configuration.
  *
  * Parameters:
  * - '''data''': The RDf data
  * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-Triples", "Turtle"
  */
@Plugin(id = "rdf", label = "RDF", description = "A Dataset where all entities are given directly in the configuration.")
case class RdfInMemoryDataset(data: String,
                              format: String,
                              @Param(label = "Clear graph before workflow execution",
                                value = "If set to true this will clear the specified graph before executing a workflow that writes to it.")
                              clearBeforeExecution: Boolean = false) extends RdfDataset with TripleSinkDataset with ClearableDatasetGraphTrait {

  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(data), null, format)

  override val sparqlEndpoint: SparqlEndpoint = new JenaModelEndpoint(model)

  /**
    * Returns a data source for reading entities from the data set.
    */
  override val source: DataSource = new SparqlSource(SparqlParams(), sparqlEndpoint)

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override val entitySink: EntitySink = new SparqlSink(SparqlParams(), sparqlEndpoint)

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override val linkSink: LinkSink = new SparqlSink(SparqlParams(), sparqlEndpoint)

  override def clear(): Unit = {
    model.removeAll()
  }

  override def tripleSink: TripleSink = new SparqlSink(SparqlParams(), sparqlEndpoint)

  override def graphToClear: String = "ignored"

  override def clearGraphBeforeExecution: Boolean = clearBeforeExecution
}
