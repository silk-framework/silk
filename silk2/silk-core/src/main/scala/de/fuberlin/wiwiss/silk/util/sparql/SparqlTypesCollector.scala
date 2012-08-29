package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.entity.SparqlRestriction

trait SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, limit: Option[Int]): Traversable[(String, Double)]
}

object SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, limit: Option[Int]) = {
    SparqlAggregateTypesCollector(endpoint, limit)
  }
}