package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import java.net.URI
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.util.sparql.{ParallelInstanceRetriever, RemoteSparqlEndpoint}

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
                       pauseTime : Int = 1000, retryCount : Int = 3, retryPause : Int = 0) extends DataSource
{
  private val uri = new URI(endpointURI)

  private val graphUri = if(graph == null) None else Some(graph)

  private val instanceUris = Option(instanceList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) =
  {
    val loginComplete =  if(login != null) Some((login, password)) else None

    val endpoint = new RemoteSparqlEndpoint(uri, instanceSpec.prefixes, loginComplete, pageSize, pauseTime, retryCount, pauseTime)

    val instanceRetriever = new ParallelInstanceRetriever(endpoint, pageSize, graphUri)

    instanceRetriever.retrieve(instanceSpec, instanceUris union instances)
  }

  override def toString = endpointURI
}
