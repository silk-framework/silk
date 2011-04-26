package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.net.URI
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.Restrictions
import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification}
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlSamplePathsCollector, SparqlAggregatePathsCollector, InstanceRetriever, RemoteSparqlEndpoint}
import java.util.logging.{Level, Logger}

/**
 * DataSource which retrieves all instances from a SPARQL endpoint
 *
 * Parameters:
 * - '''endpointURI''': The URI of the SPARQL endpoint
 * - '''login (optional)''': Login required for authentication
 * - '''password (optional)''': Password required for authentication
 * - '''graph (optional)''': Only retrieve instances from a specific graph
 * - '''pageSize (optional)''': The number of solutions to be retrieved per SPARQL query (default: 1000)
 * - '''instanceList (optional)''': A list of instances to be retrieved. If not given, all instances will be retrieved. Multiple instances are separated by a space.
 * - '''pauseTime (optional)''': The number of milliseconds to wait between subsequent query 
 * - '''retryCount (optional)''': The number of retires if a query fails
 * - '''retryPause (optional)''': The number of milliseconds to wait until a failed query is retried
 */
@StrategyAnnotation(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "DataSource which retrieves all instances from a SPARQL endpoint")
class SparqlDataSource(endpointURI : String, login : String = null, password : String = null,
                       graph : String = null, pageSize : Int = 1000, instanceList : String = null,
                       pauseTime : Int = 0, retryCount : Int = 3, retryPause : Int = 1000) extends DataSource
{
  private val uri = new URI(endpointURI)

  private val loginComplete = if(login != null) Some((login, password)) else None

  private val graphUri = if(graph == null) None else Some(graph)

  private val instanceUris = Option(instanceList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  private val logger = Logger.getLogger(SparqlDataSource.getClass.getName)

  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) =
  {
    val instanceRetriever = InstanceRetriever(createEndpoint(), pageSize, graphUri)

    instanceRetriever.retrieve(instanceSpec, instanceUris union instances)
  }

  override def retrievePaths(restrictions : Restrictions, depth : Int, limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, 3, 1000)

    try
    {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
    }
    catch
    {
      case ex : Exception =>
      {
        logger.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)

        SparqlSamplePathsCollector(createEndpoint(), restrictions, limit)
      }
    }
  }

  protected def createEndpoint() =
  {
    new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, retryCount, retryPause)
  }

  override def toString = endpointURI
}
