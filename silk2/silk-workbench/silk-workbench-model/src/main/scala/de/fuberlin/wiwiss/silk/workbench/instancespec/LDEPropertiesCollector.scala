package de.fuberlin.wiwiss.silk.workbench.instancespec

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.instance.{Path, ForwardOperator}
import de.fuberlin.wiwiss.silk.workbench.util.QueryFactory
import de.fuberlin.wiwiss.silk.linkspec.Restrictions

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
  def apply(endpoint : SparqlEndpoint, restrictions : Restrictions) : Traversable[(Path, Double)] =
  {
    getAllPaths(endpoint, restrictions)
  }

  private def getAllPaths(endpoint : SparqlEndpoint, restrictions : Restrictions) : Traversable[(Path, Double)] =
  {
    val variable = restrictions.toSparql.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')
    val category = restrictions.toString.split(' ')(2)

    val sparql = QueryFactory.sPropertyPaths(category)

    val results = endpoint.query(sparql, MaxPropertyCount).toList
    if(!results.isEmpty)
    {
      for(result <- results if result.contains("p")) yield
      {
        (new Path(variable, ForwardOperator(Uri.fromURI(result("p").value)) :: Nil), 1.)
      }
    }
    else
    {
      Traversable.empty
    }
  }
    
}