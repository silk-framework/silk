package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset.rdf.{EntityRetrieverStrategy, RdfDataset, SparqlParams}
import org.silkframework.dataset.{DatasetCategories, TripleSink, TripleSinkDataset}
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.{MultilineStringParameter, PasswordParameter}

@Plugin(
  id = "sparqlEndpoint",
  label = "SPARQL endpoint",
  categories = Array(DatasetCategories.remote),
  description = "Connect to an existing SPARQL endpoint.")
case class SparqlDataset(
  @Param(label = "Endpoint URI", value = "The URI of the SPARQL endpoint, e.g., http://dbpedia.org/sparql")
  endpointURI: String,
  @Param("Login required for authentication")
  login: String = null,
  @Param("Password required for authentication")
  password: PasswordParameter = PasswordParameter.empty,
  @Param("Only retrieve entities from a specific graph")
  graph: String = null,
  @Param(value = "The number of solutions to be retrieved per SPARQL query.", advanced = true)
  pageSize: Int = 1000,
  @Param(value = "A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by whitespace.", advanced = true)
  entityList: MultilineStringParameter = MultilineStringParameter(""),
  @Param(value = "The number of milliseconds to wait between subsequent query", advanced = true)
  pauseTime: Int = 0,
  @Param(value = "The number of retries if a query fails", advanced = true)
  retryCount: Int = 3,
  @Param(value = "The number of milliseconds to wait until a failed query is retried.", advanced = true)
  retryPause: Int = 1000,
  @Param(value = "Additional parameters to be appended to every request e.g. &soft-limit=1", advanced = true)
  queryParameters: String = "",
  @Param("The strategy use for retrieving entities: simple: Retrieve all entities using a single query; subQuery: Use a single query, but wrap it for improving the performance on Virtuoso; parallel: Use a separate Query for each entity property.")
  strategy: EntityRetrieverStrategy = EntityRetrieverStrategy.parallel,
  @Param("Include useOrderBy in queries to enforce correct order of values.")
  useOrderBy: Boolean = true,
  @Param(label = "Clear graph before workflow execution (deprecated)",
    value = "This is deprecated, use the 'Clear dataset' operator instead to clear a dataset in a workflow. If set to true this will clear the specified graph before executing a workflow that writes to it.",
    advanced = true)
  clearGraphBeforeExecution: Boolean = false,
  @Param(
    label = "SPARQL query timeout (ms)",
    value = "SPARQL query timeout (select/update) in milliseconds. A value of zero means that the timeout configured via " +
        "property is used (e.g. configured via silk.remoteSparqlEndpoint.defaults.read.timeout.ms). To overwrite the configured value specify a value greater than zero.")
  sparqlTimeout: Int = 0) extends RdfDataset with TripleSinkDataset {

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
      useOrderBy = useOrderBy,
      timeout = Some(sparqlTimeout).filter(_ > 0)
    )

  override def graphOpt: Option[String] = Option(graph).filterNot(_.isEmpty)

  override val sparqlEndpoint: RemoteSparqlEndpoint = {
    RemoteSparqlEndpoint(params)
  }

  override def source(implicit userContext: UserContext) = new SparqlSource(params, sparqlEndpoint)

  override def linkSink(implicit userContext: UserContext) = new SparqlSink(params, sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)

  override def entitySink(implicit userContext: UserContext) = new SparqlSink(params, sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)

  override def tripleSink(implicit userContext: UserContext): TripleSink = new SparqlSink(params, sparqlEndpoint, dropGraphOnClear = clearGraphBeforeExecution)
}
