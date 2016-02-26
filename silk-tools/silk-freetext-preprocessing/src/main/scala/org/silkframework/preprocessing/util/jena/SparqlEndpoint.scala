package org.silkframework.preprocessing.util.jena

import com.hp.hpl.jena.query.{QueryExecutionFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.Model

import scala.collection.JavaConversions._

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
        (for (varName <- result.varNames.toList;
             value <- Option(result.get(varName))) yield (varName, toSilkNode(value))).toMap
      }

    results.toList
  }


  /**
   *  Converts a Jena RDFNode to a Silk Node.
   */
  private def toSilkNode(node: com.hp.hpl.jena.rdf.model.RDFNode) = node match {
    case r: com.hp.hpl.jena.rdf.model.Resource if !r.isAnon => org.silkframework.util.sparql.Resource(r.getURI)
    case r: com.hp.hpl.jena.rdf.model.Resource => org.silkframework.util.sparql.BlankNode(r.getId.getLabelString)
    case l: com.hp.hpl.jena.rdf.model.Literal => org.silkframework.util.sparql.Literal(l.getString)
    case _ => throw new IllegalArgumentException("Unsupported Jena RDFNode type '" + node.getClass.getName + "' in Jena SPARQL results")
  }

}
