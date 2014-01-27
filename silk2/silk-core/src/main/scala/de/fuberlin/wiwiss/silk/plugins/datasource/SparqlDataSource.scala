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

package de.fuberlin.wiwiss.silk.plugins.datasource

import de.fuberlin.wiwiss.silk.datasource.{DataSource}
import java.net.URI
import de.fuberlin.wiwiss.silk.util.plugin.{ResourceLoader, Plugin}
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path, EntityDescription}
import de.fuberlin.wiwiss.silk.util.sparql._

/**
 * DataSource which retrieves all entities from a SPARQL endpoint
 *
 * Parameters:
 * - '''endpointURI''': The URI of the SPARQL endpoint e.g. http://dbpedia.org/sparql
 * - '''login (optional)''': Login required for authentication
 * - '''password (optional)''': Password required for authentication
 * - '''graph (optional)''': Only retrieve entities from a specific graph
 * - '''pageSize (optional)''': The number of solutions to be retrieved per SPARQL query (default: 1000)
 * - '''entityList (optional)''': A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by a space.
 * - '''pauseTime (optional)''': The number of milliseconds to wait between subsequent query 
 * - '''retryCount (optional)''': The number of retires if a query fails
 * - '''retryPause (optional)''': The number of milliseconds to wait until a failed query is retried
 * - '''queryParameters (optional)''' Additional parameters to be appended to every request e.g. &soft-limit=1
 * - '''parallel (optional)''' True (default), if multiple queries should be executed in parallel for faster retrieval.
 */
@Plugin(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "DataSource which retrieves all entities from a SPARQL endpoint")
case class SparqlDataSource(endpointURI: String, login: String = null, password: String = null,
                            graph: String = null, pageSize: Int = 1000, entityList: String = null,
                            pauseTime: Int = 0, retryCount: Int = 3, retryPause: Int = 1000,
                            queryParameters: String = "", parallel: Boolean = true) extends DataSource {
  private val uri = new URI(endpointURI)

  private val loginComplete = {
    if (login != null) {
      require(password != null, "No password provided for login '" + login + "'. Please set the 'password' parameter.")
      Some((login, password))
    } else {
      None
    }
  }

  private val graphUri = if (graph == null) None else Some(graph)

  private val entityUris = Option(entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  private val logger = Logger.getLogger(SparqlDataSource.getClass.getName)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    val entityRetriever =
      if(parallel)
        new ParallelEntityRetriever(createEndpoint(), pageSize, graphUri)
      else
        new SimpleEntityRetriever(createEndpoint(), pageSize, graphUri)

    entityRetriever.retrieve(entityDesc, entityUris union entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, 3, 1000, queryParameters)

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
    } catch {
      case ex: Exception =>
        logger.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(createEndpoint(), restrictions, limit)
    }
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(createEndpoint(), limit)
  }

  protected def createEndpoint() = {
    new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, retryCount, retryPause, queryParameters)
  }

  override def toString = endpointURI
}
