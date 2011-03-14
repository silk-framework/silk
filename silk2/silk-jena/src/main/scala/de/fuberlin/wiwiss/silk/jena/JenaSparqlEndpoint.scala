package de.fuberlin.wiwiss.silk.jena

import scala.collection.JavaConversions._
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.query.{QuerySolution, ResultSet, QueryExecutionFactory}
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlEndpoint, Node}

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
private class JenaSparqlEndpoint(model : Model) extends SparqlEndpoint
{
  /**
   * Executes a SPARQL SELECT query.
   */
  override def query(sparql : String, limit : Int) : Traversable[Map[String, Node]] =
  {
    val qe = QueryExecutionFactory.create(sparql + " LIMIT " + limit, model)

    try
    {
      toSilkResults(qe.execSelect())
    }
    finally
    {
      qe.close()
    }
  }

  /**
   * Converts a Jena ARQ ResultSet to a Silk ResultSet.
   */
  private def toSilkResults(resultSet : ResultSet) =
  {
    val results =
      for(result <- resultSet) yield
      {
        toSilkBinding(result)
      }

    results.toList
  }

  /**
   * Converts a Jena ARQ QuerySolution to a Silk binding
   */
  private def toSilkBinding(querySolution : QuerySolution) =
  {
    val values =
      for(varName <- querySolution.varNames.toList;
          value <- Option(querySolution.get(varName))) yield
      {
        (varName, toSilkNode(value))
      }

    values.toMap
  }

  /**
   *  Converts a Jena RDFNode to a Silk Node.
   */
  private def toSilkNode(node : com.hp.hpl.jena.rdf.model.RDFNode) = node match
  {
    case r : com.hp.hpl.jena.rdf.model.Resource if !r.isAnon => de.fuberlin.wiwiss.silk.util.sparql.Resource(r.getURI)
    case r : com.hp.hpl.jena.rdf.model.Resource => de.fuberlin.wiwiss.silk.util.sparql.BlankNode(r.getId.getLabelString)
    case l : com.hp.hpl.jena.rdf.model.Literal => de.fuberlin.wiwiss.silk.util.sparql.Literal(l.getString)
    case _ => throw new IllegalArgumentException("Unsupported Jena RDFNode type '" + node.getClass.getName + "' in Jena SPARQL results")
  }
}