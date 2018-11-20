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

package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.StringWriter
import java.util.logging.{Level, Logger}

import org.apache.jena.query._
import org.apache.jena.update.UpdateProcessor
import org.silkframework.dataset.rdf.{RdfNode, SparqlEndpoint, SparqlResults => SilkResultSet}
import org.silkframework.runtime.activity.UserContext

import scala.collection.immutable.SortedMap

/**
 * A SPARQL endpoint which executes all queries using Jena.
 */
abstract class JenaEndpoint extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[JenaEndpoint].getName)

  /**
   * Overloaded in subclasses.
   */
  protected def createQueryExecution(query: Query): QueryExecution

  /**
    * Overloaded in subclasses.
    */
  protected def createUpdateExecution(query: String): UpdateProcessor

  protected def addGraph(query: Query): Unit = {
    sparqlParams.graph foreach { graphURI =>
      query.addGraphURI(graphURI)
    }
  }

  /**
   * Executes a SPARQL SELECT query.
   */
  override def select(sparql: String, limit: Int)
                     (implicit userContext: UserContext): SilkResultSet = synchronized {
    val query = QueryFactory.create(sparql)
    // Log query
    if (logger.isLoggable(Level.FINE)) logger.fine("Executing query:\n" + sparql)
    if(limit < Int.MaxValue) {
      query.setLimit(limit)
    }
    if(PagingSparqlTraversable.graphPatternRegex.findFirstIn(sparql).isEmpty) {
      addGraph(query)
    }
    // Execute query
//    val query = if(limit < Int.MaxValue) sparql + " LIMIT " + limit else sparql
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
  override def construct(query: String)
                        (implicit userContext: UserContext): String = synchronized {
    val qe = createQueryExecution(QueryFactory.create(query))
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
  override def update(query: String)
                     (implicit userContext: UserContext): Unit = synchronized {
    createUpdateExecution(query).execute()
  }

  /**
   * Converts a Jena ARQ ResultSet to a Silk ResultSet.
   */
  private def toSilkResults(resultSet: ResultSet) = {
    val results =
      new Traversable[SortedMap[String, RdfNode]] {
        override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
          JenaResultsReader.read(resultSet, f)
        }
      }

    SilkResultSet(results.toList)
  }

}