package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql

import de.fuberlin.wiwiss.silk.dataset.rdf.SparqlEndpoint

trait SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, limit: Option[Int]): Traversable[(String, Double)]
}

object SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, limit: Option[Int]) = {
    SparqlAggregateTypesCollector(endpoint, limit)
  }
}