package de.fuberlin.wiwiss.silk.preprocessing.util.jena

import scala.collection.JavaConversions._
import de.fuberlin.wiwiss.silk.util.sparql.Node
import com.hp.hpl.jena.query.{QuerySolution, ResultSet, QueryExecutionFactory}
import com.hp.hpl.jena.rdf.model.Model

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
class SparqlEndpoint(model:Model) {
  /**
   * Executes a SPARQL SELECT query.
   */
  def query(sparql: String, limit: Int = Integer.MAX_VALUE): Traversable[Map[String, Node]] = {
    val qe = QueryExecutionFactory.create(sparql + " LIMIT " + limit, model)

    try {
      mapResults(qe.execSelect())
    }
    finally {
      qe.close()
    }
  }

  /**
   * Converts a Jena ARQ ResultSet to a ResultSet.
   */
  private def mapResults(resultSet: ResultSet) = {
    val results =
      for (result <- resultSet) yield {
        toBinding(result)
      }

    results.toList
  }

  /**
   * Converts a Jena ARQ QuerySolution to a entity binding
   */
  private def toBinding(querySolution: QuerySolution) = {
    val values =
      for (varName <- querySolution.varNames.toList;
           value <- Option(querySolution.get(varName))) yield {
        (varName, toSilkNode(value))
      }

    values.toMap
  }

  /**
   *  Converts a Jena RDFNode to a Silk Node.
   */
  private def toSilkNode(node: com.hp.hpl.jena.rdf.model.RDFNode) = node match {
    case r: com.hp.hpl.jena.rdf.model.Resource if !r.isAnon => de.fuberlin.wiwiss.silk.util.sparql.Resource(r.getURI)
    case r: com.hp.hpl.jena.rdf.model.Resource => de.fuberlin.wiwiss.silk.util.sparql.BlankNode(r.getId.getLabelString)
    case l: com.hp.hpl.jena.rdf.model.Literal => de.fuberlin.wiwiss.silk.util.sparql.Literal(l.getString)
    case _ => throw new IllegalArgumentException("Unsupported Jena RDFNode type '" + node.getClass.getName + "' in Jena SPARQL results")
  }

}
