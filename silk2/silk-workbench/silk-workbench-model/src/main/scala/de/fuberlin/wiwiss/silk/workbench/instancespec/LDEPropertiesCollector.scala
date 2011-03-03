package de.fuberlin.wiwiss.silk.workbench.instancespec

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.instance.{Path, ForwardOperator}
import de.fuberlin.wiwiss.silk.workbench.util.QueryFactory

/**
 * Retrieves property paths from the Wiki ontology
 */
object LDEPropertiesCollector
{
  private val logger = Logger.getLogger(LDEPropertiesCollector.getClass.getName)

  /** The maximum number of properties */
  private val MaxPropertyCount = 50

  /**
   * Retrieves a list of properties which are defined on most instances.
   */
  def apply(endpoint : SparqlEndpoint, restriction : String) : Traversable[(Path, Double)] =
  {
    getAllPaths(endpoint, restriction)
  }

  private def getAllPaths(endpoint : SparqlEndpoint, restriction : String) : Traversable[(Path, Double)] =
  {
    val variable = restriction.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')
    val category = restriction.split(' ')(2)

    val sparql = QueryFactory.sPropertyPaths(category)

    val results = endpoint.query(sparql, MaxPropertyCount).toList
    if(!results.isEmpty)
    {
      for(result <- results if result.contains("p")) yield
      {
        (new Path(variable, ForwardOperator(Uri.fromURI(result("p").value, endpoint.prefixes)) :: Nil), 1.)
      }
    }
    else
    {
      Traversable.empty
    }
  }
    
}