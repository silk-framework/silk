package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.dataset.rdf.{EntityRetrieverStrategy, RdfDataset, SparqlParams}
import org.silkframework.dataset.{DatasetCategories, TripleSink, TripleSinkDataset}
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.plugins.dataset.rdf.tasks.{SparqlSelectCustomTask, SparqlUpdateCustomTask}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.plugin.types.{MultilineStringParameter, PasswordParameter}

@Plugin(
  id = SparqlDataset.pluginId,
  label = "SPARQL endpoint",
  categories = Array(DatasetCategories.remote),
  description = "Connects to an existing SPARQL endpoint.",
  documentationFile = "SparqlDataset.md",
  relatedPlugins = Array(
    new PluginReference(
      id = InMemoryDataset.pluginId,
      description = "The SPARQL endpoint dataset reads from and writes to a remote endpoint that retains its contents between runs. The in-memory dataset starts empty every time the workflow runs and loses all its data when execution finishes — the two are not alternatives for the same storage need."
    ),
    new PluginReference(
      id = RdfFileDataset.pluginId,
      description = "The RDF file dataset loads its contents from a file into memory at read time and supports only N-Triples as output. The SPARQL endpoint dataset connects to a remote endpoint that handles queries and updates without loading the full dataset into process memory."
    ),
    new PluginReference(
      id = SparqlUpdateCustomTask.pluginId,
      description = "The SPARQL Update query plugin generates SPARQL Update statements from entity input using a template; the SPARQL endpoint dataset is what those statements are written to. One produces the queries, the other executes them against the endpoint."
    ),
    new PluginReference(
      id = SparqlSelectCustomTask.pluginId,
      description = "The SPARQL Select query plugin reads from a SPARQL endpoint dataset by executing a SELECT query against it; the SPARQL Update query plugin writes to the same kind of dataset by sending update statements to it. The two plugins sit on opposite ends of the same data flow."
    )
  )
)
case class SparqlDataset(
  @Param(label = "Endpoint URI", value = "The URI of the SPARQL endpoint, e.g. `http://dbpedia.org/sparql`")
  endpointURI: String,
  @Param("Login required for authentication")
  login: String = null,
  @Param("Password required for authentication")
  password: PasswordParameter = PasswordParameter.empty,
  @Param("The URI of a named graph. If set, the SPARQL endpoint will only retrieve entities from that specific graph.")
  graph: String = null,
  @Param(value = "The number of entities to be retrieved per SPARQL query. This is the page size used when paging.", advanced = true)
  pageSize: Int = 1000,
  @Param(value = "An optional list of entities to be retrieved. If not specified, all entities will be retrieved." +
    " Multiple entities need to be separated by whitespace.", advanced = true)
  entityList: MultilineStringParameter = MultilineStringParameter(""),
  @Param(value = "The number of milliseconds to wait between subsequent queries", advanced = true)
  pauseTime: Int = 0,
  @Param(value = "The total number of retries to execute a (repeatedly) failing query", advanced = true)
  retryCount: Int = 3,
  @Param(value = "The number of milliseconds to wait until a previously failed query is executed again", advanced = true)
  retryPause: Int = 1000,
  @Param(value = "Additional parameters to be appended to every query, e.g. `&soft-limit=1`", advanced = true)
  queryParameters: String = "",
  @Param("The strategy for retrieving entities. There are three options:" +
    " `simple` retrieves all entities using a single query;" +
    " `subQuery` also uses a single query, which is optimized for Virtuoso;" +
    " `parallel` executes multiple queries in parallel, one for each entity property.")
  strategy: EntityRetrieverStrategy = EntityRetrieverStrategy.parallel,
  @Param("Enforces the correct ordering of values, if set to `true` (default).")
  useOrderBy: Boolean = true,
  @Param(label = "Clear graph before workflow execution (deprecated)",
    value = "This is deprecated, use the 'Clear dataset' operator instead to clear a dataset in a workflow. If set to `true`, this will clear the specified graph before executing a workflow that writes into it.",
    advanced = true)
  clearGraphBeforeExecution: Boolean = false,
  @Param(
    label = "SPARQL query timeout (ms)",
    value = "SPARQL query timeout in milliseconds." +
      " By default, a value of zero is used." +
      " This zero value has a symbolic character: it means that the timeout of SPARQL select and update queries is" +
      " configured via the properties `silk.remoteSparqlEndpoint.defaults.connection.timeout.ms and" +
      " `silk.remoteSparqlEndpoint.defaults.read.timeout.ms` for the default connection and read timeouts." +
      " To overwrite these configured values, specify a (common) timeout greater than zero milliseconds.")
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

object SparqlDataset {
  final val pluginId = "sparqlEndpoint"
}
