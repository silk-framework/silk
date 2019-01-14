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

import java.util.logging.{Level, Logger}

import org.apache.jena.graph.Node
import org.apache.jena.query._
import org.apache.jena.rdf.model.{Model, Statement}
import org.apache.jena.sparql.core.{Quad => JenaQuad}
import org.apache.jena.update.UpdateProcessor
import org.silkframework.dataset.rdf._
import org.silkframework.runtime.activity.UserContext

import scala.collection.JavaConverters._
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
                     (implicit userContext: UserContext): SparqlResults = synchronized {
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
                        (implicit userContext: UserContext): QuadIterator = synchronized {

    val qe = createQueryExecution(QueryFactory.create(query))
    val quadIterator = qe.execConstructQuads()
    val results = new QuadIterator(
      quadIterator.hasNext,
      () => JenaEndpoint.quadToTuple(quadIterator.next())
    )
    //qe.close()
    results
  }

  /**
    * Executes a construct query and returns a Jena Model
    */
  def constructModel(query: String)
                    (implicit userContext: UserContext): Model = synchronized {

    val qe = createQueryExecution(QueryFactory.create(query))
    val model = qe.execConstruct()
    qe.close()
    model
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

    SparqlResults(results.toList)
  }

}

object JenaEndpoint{
  // TODO needs test
  private def getObject(n: Node): RdfNode ={
    if(n.isBlank){
      BlankNode(n.getBlankNodeLabel)
    }
    else if(n.isLiteral) {
      if(n.getLiteralLanguage != null && n.getLiteralLanguage.nonEmpty){
        LanguageLiteral(n.getLiteral.getLexicalForm, n.getLiteralLanguage)
      }
      else if(n.getLiteralDatatype != null){
        DataTypeLiteral(n.getLiteral.getLexicalForm, n.getLiteralDatatypeURI)
      }
      else{
        PlainLiteral(n.getLiteral.getLexicalForm)
      }
    }
    else{
      Resource(n.getURI)
    }
  }

  private[endpoint] def quadToTuple(q: JenaQuad): Quad = {
    val subj = q.getSubject
    if(subj.isBlank){
      Quad(BlankNode(subj.getBlankNodeLabel), Resource(q.getPredicate.getURI), getObject(q.getObject), Option(q.getGraph).map(g => Resource(g.getURI)))
    }
    else{
      Quad(Resource(subj.getURI), Resource(q.getPredicate.getURI), getObject(q.getObject), Option(q.getGraph).map(g => Resource(g.getURI)))
    }
  }

  private[endpoint] def statementToTuple(q: Statement) = {
    val subj = q.getSubject.asNode()
    if(subj.isBlank){
      Quad(BlankNode(subj.getBlankNodeLabel), Resource(q.getPredicate.getURI), getObject(q.getObject.asNode()), None)
    }
    else{
      Quad(Resource(subj.getURI), Resource(q.getPredicate.getURI), getObject(q.getObject.asNode()), None)
    }
  }
}
