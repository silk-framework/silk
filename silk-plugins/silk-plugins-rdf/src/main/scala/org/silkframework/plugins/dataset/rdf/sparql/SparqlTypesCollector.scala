package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint

trait SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, graph: Option[String], limit: Option[Int]): Traversable[(String, Double)]
}

object SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, graph: Option[String], limit: Option[Int]) = {
    SparqlAggregateTypesCollector(endpoint, graph, limit)
  }
}