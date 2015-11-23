package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.rdf.RdfDatasetPlugin
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.runtime.plugin.Plugin

/**
 * Dataset which retrieves and writes to all entities from/to a SPARQL endpoint
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
@Plugin(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "Dataset which retrieves all entities from a SPARQL endpoint")
case class SparqlDataset(endpointURI: String, login: String = null, password: String = null,
                         graph: String = null, pageSize: Int = 1000, entityList: String = null,
                         pauseTime: Int = 0, retryCount: Int = 3, retryPause: Int = 1000,
                         queryParameters: String = "", parallel: Boolean = true, useOrderBy: Boolean = true) extends RdfDatasetPlugin {

  private val params =
    SparqlParams(
      uri = endpointURI,
      user = login,
      password = password,
      graph = Option(graph).filterNot(_.isEmpty),
      pageSize = pageSize,
      entityList = entityList,
      pauseTime = pauseTime,
      retryCount = retryCount,
      retryPause = retryPause,
      queryParameters = queryParameters,
      parallel = parallel,
      useOrderBy = useOrderBy
    )

  override val sparqlEndpoint = {
    //new JenaRemoteEndpoint(endpointURI)
    new RemoteSparqlEndpoint(params)
  }

  override val source = new SparqlSource(params, sparqlEndpoint)

  override val sink = new SparqlSink(params, sparqlEndpoint)
}
