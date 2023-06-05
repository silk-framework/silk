package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext

trait SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, graph: Option[String], limit: Option[Int])
           (implicit userContext: UserContext): Iterable[(String, Double)]
}

object SparqlTypesCollector {
  def apply(endpoint: SparqlEndpoint, graph: Option[String], limit: Option[Int])
           (implicit userContext: UserContext): Iterable[(String, Double)] = {
    SparqlAggregateTypesCollector(endpoint, graph, limit)
  }
}