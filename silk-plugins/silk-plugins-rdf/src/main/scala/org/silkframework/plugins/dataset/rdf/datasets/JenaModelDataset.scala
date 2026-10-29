package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint

/**
  * An in-memory RDF dataset backed by a Jena model. Used programmatically (no plugin registration);
  * accessed through [[JenaModelDatasetExecutor]].
  */
case class JenaModelDataset(dropGraphOnClear: Boolean = true) extends RdfDataset {

  @volatile
  var sparqlEndpoint: SparqlEndpoint = {
    new JenaModelEndpoint(ModelFactory.createDefaultModel())
  }
}

object JenaModelDataset {

  def fromModel(model: Model, dropGraphOnClear: Boolean = true): JenaModelDataset = {
    val ds = new JenaModelDataset(dropGraphOnClear)
    ds.sparqlEndpoint = new JenaModelEndpoint(model)
    ds
  }

  /** Wraps an existing endpoint as a JenaModelDataset, preserving the endpoint's identity and state. */
  def fromEndpoint(endpoint: SparqlEndpoint, dropGraphOnClear: Boolean = true): JenaModelDataset = {
    val ds = new JenaModelDataset(dropGraphOnClear)
    ds.sparqlEndpoint = endpoint
    ds
  }

}

