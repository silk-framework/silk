/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import java.io.StringWriter
import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.query.{QueryExecution, QuerySolution, ResultSet}
import com.hp.hpl.jena.update.UpdateProcessor
import de.fuberlin.wiwiss.silk.dataset.rdf.{BlankNode, Literal, Resource, SparqlEndpoint, SparqlResults => SilkResultSet}

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedMap

/**
 * A SPARQL endpoint which executes all queries using Jena.
 */
abstract class JenaEndpoint extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[JenaEndpoint].getName)

  /**
   * Overloaded in subclasses.
   */
  protected def createQueryExecution(query: String): QueryExecution

  /**
    * Overloaded in subclasses.
    */
  protected def createUpdateExecution(query: String): UpdateProcessor

  /**
   * Executes a SPARQL SELECT query.
   */
  override def select(sparql: String, limit: Int): SilkResultSet = {
    // Log query
    if (logger.isLoggable(Level.FINE)) logger.fine("Executing query:\n" + sparql)
    // Execute query
    val query = if(limit < Int.MaxValue) sparql + " LIMIT " + limit else sparql
    val qe = createQueryExecution(query)
    try {
      toSilkResults(qe.execSelect())
    }
    finally {
      qe.close()
    }
  }

  /**
    * Executes a construct query.
    */
  override def construct(query: String): String = {
    val qe = createQueryExecution(query)
    try {
      val resultModel = qe.execConstruct()
      val writer = new StringWriter()
      resultModel.write(writer, "Turtle")
      writer.toString
    }
    finally {
      qe.close()
    }
  }

  /**
    * Executes an update query.
    */
  override def update(query: String): Unit = {
    createUpdateExecution(query).execute()
  }

  /**
   * Converts a Jena ARQ ResultSet to a Silk ResultSet.
   */
  private def toSilkResults(resultSet: ResultSet) = {
    val results =
      for (result <- resultSet) yield {
        toSilkBinding(result)
      }

    SilkResultSet(results.toList)
  }

  /**
   * Converts a Jena ARQ QuerySolution to a Silk binding
   */
  private def toSilkBinding(querySolution: QuerySolution) = {
    val values =
      for (varName <- querySolution.varNames.toList;
           value <- Option(querySolution.get(varName))) yield {
        (varName, toSilkNode(value))
      }

    SortedMap(values: _*)
  }

  /**
   *  Converts a Jena RDFNode to a Silk Node.
   */
  private def toSilkNode(node: com.hp.hpl.jena.rdf.model.RDFNode) = node match {
    case r: com.hp.hpl.jena.rdf.model.Resource if !r.isAnon => Resource(r.getURI)
    case r: com.hp.hpl.jena.rdf.model.Resource => BlankNode(r.getId.getLabelString)
    case l: com.hp.hpl.jena.rdf.model.Literal => Literal(l.getString)
    case _ => throw new IllegalArgumentException("Unsupported Jena RDFNode type '" + node.getClass.getName + "' in Jena SPARQL results")
  }
}