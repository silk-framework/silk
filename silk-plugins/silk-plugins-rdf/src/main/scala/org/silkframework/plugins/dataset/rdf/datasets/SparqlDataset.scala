package org.silkframework.plugins.dataset.rdf.datasets

import java.io.InputStream

import org.silkframework.dataset.rdf.{EntityRetrieverStrategy, RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.dataset.{TripleSink, TripleSinkDataset}
import org.silkframework.plugins.dataset.rdf.endpoint.{JenaModelEndpoint, RemoteSparqlEndpoint}
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, PasswordParameter, Plugin}

@Plugin(id = "sparqlEndpoint", label = "SPARQL endpoint (remote)", description = "Dataset which retrieves all entities from a SPARQL endpoint")
case class SparqlDataset(
  @Param(label = "endpoint URI", value = "The URI of the SPARQL endpoint e.g. http://dbpedia.org/sparql")
  endpointURI: String,
  @Param("Login required for authentication")
  login: String = null,
  @Param("Password required for authentication")
  password: PasswordParameter = PasswordParameter(null),
  @Param("Only retrieve entities from a specific graph")
  graph: String = null,
  @Param("The number of solutions to be retrieved per SPARQL query.")
  pageSize: Int = 1000,
  @Param("A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by whitespace.")
  entityList: MultilineStringParameter = MultilineStringParameter(""),
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
  useOrderBy: Boolean = true,
  @Param(label = "Clear graph before workflow execution",
    value = "If set to true this will clear the specified graph before executing a workflow that writes to it.")
  clearGraphBeforeExecution: Boolean = true) extends RdfDataset with TripleSinkDataset {

  private val params =
    SparqlParams(
      uri = endpointURI,
      user = login,
      password = password.decryptedString,
      graph = Option(graph).filterNot(_.isEmpty),
      pageSize = pageSize,
      entityList = entityList.str,
      pauseTime = pauseTime,
      retryCount = retryCount,
      retryPause = retryPause,
      queryParameters = queryParameters,
      strategy = strategy,
      useOrderBy = useOrderBy
    )

  override def sparqlEndpoint(sparqlInputStream: Option[InputStream] = None): SparqlEndpoint = {
    RemoteSparqlEndpoint(params)
  }

  override def source(implicit userContext: UserContext) = new SparqlSource(params, sparqlEndpoint())

  override def linkSink(implicit userContext: UserContext) = new SparqlSink(params, sparqlEndpoint(), dropGraphOnClear = clearGraphBeforeExecution)

  override def entitySink(implicit userContext: UserContext) = new SparqlSink(params, sparqlEndpoint(), dropGraphOnClear = clearGraphBeforeExecution)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(params, sparqlEndpoint(), dropGraphOnClear = clearGraphBeforeExecution)
}
