package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.instance.{SparqlRestriction, Path}

/**
 * Retrieves the most frequent property paths.
 */
trait SparqlPathsCollector {
  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]): Traversable[(Path, Double)]
}

object SparqlPathsCollector {
  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]) = {
    SparqlSamplePathsCollector(endpoint, restrictions, limit)
  }
}