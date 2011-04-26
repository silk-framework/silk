package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.linkspec.Restrictions
import de.fuberlin.wiwiss.silk.instance.Path

/**
 * Retrieves the most frequent property paths.
 */
trait  SparqlPathsCollector
{
  def apply(endpoint : SparqlEndpoint, restrictions : Restrictions, limit : Option[Int]) : Traversable[(Path, Double)]
}