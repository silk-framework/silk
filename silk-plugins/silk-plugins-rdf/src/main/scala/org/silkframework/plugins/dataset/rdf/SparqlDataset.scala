package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.{TripleSink, TripleSinkDataset}
import org.silkframework.dataset.rdf.{EntityRetrieverStrategy, RdfDataset, SparqlParams}
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.runtime.plugin.{Param, Plugin}

@Plugin(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "Dataset which retrieves all entities from a SPARQL endpoint")
case class SparqlDataset(
  @Param(label = "endpoint URI", value = "The URI of the SPARQL endpoint e.g. http://dbpedia.org/sparql")
  endpointURI: String,
  @Param("Login required for authentication")
  login: String = null,
  @Param("Password required for authentication")
  password: String = null,
  @Param("Only retrieve entities from a specific graph")
  graph: String = null,
  @Param("The number of solutions to be retrieved per SPARQL query.")
  pageSize: Int = 1000,
  @Param("A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by a space.")
  entityList: String = null,
  @Param("The number of milliseconds to wait between subsequent query")
  pauseTime: Int = 0,
  @Param("The number of retries if a query fails")
  retryCount: Int = 3,
  @Param("The number of milliseconds to wait until a failed query is retried.")
  retryPause: Int = 1000,
  @Param("Additional parameters to be appended to every request e.g. &soft-limit=1")
  queryParameters: String = "",
  @Param("The strategy use for retrieving entities: simple: Retrieve all entities using a single query; subQuery: Use a single query, but wrap it for improving the performance on Virtuoso; parallel: Use a separate Query for each entity property.")
  strategy: EntityRetrieverStrategy = EntityRetrieverStrategy.parallel,
  @Param("Include useOrderBy in queries to enforce correct order of values.")
  useOrderBy: Boolean = true) extends RdfDataset with TripleSinkDataset {

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
      strategy = strategy,
      useOrderBy = useOrderBy
    )

  override val sparqlEndpoint = {
    //new JenaRemoteEndpoint(endpointURI)
    new RemoteSparqlEndpoint(params)
  }

  override val source = new SparqlSource(params, sparqlEndpoint)

  override val linkSink = new SparqlSink(params, sparqlEndpoint)

  override val entitySink = new SparqlSink(params, sparqlEndpoint)

  override def clear() = {
    for(graph <- params.graph)
      sparqlEndpoint.update(s"DROP SILENT GRAPH <$graph>")
  }

  override def tripleSink: TripleSink = new SparqlSink(params, sparqlEndpoint)
}
