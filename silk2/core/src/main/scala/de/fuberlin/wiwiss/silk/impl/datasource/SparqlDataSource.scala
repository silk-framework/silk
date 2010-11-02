package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.sparql.{InstanceRetriever, RemoteSparqlEndpoint}
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import java.net.URI

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
class SparqlDataSource(val params : Map[String, String]) extends DataSource
{
  private val endpoint = new RemoteSparqlEndpoint(
    uri = new URI(readRequiredParam("endpointURI")),
    login = readOptionalParam("login").map(login => (login, readRequiredParam("password"))),
    pauseTime = readOptionalIntParam("pauseTime").getOrElse(0),
    retryCount = readOptionalIntParam("retryCount").getOrElse(3),
    initialRetryPause = readOptionalIntParam("retryPause").getOrElse(1000)
  )

  private val graphUri = readOptionalParam("graph")

  private val pageSize = readOptionalIntParam("pageSize").getOrElse(1000)

  private val instanceList = readOptionalParam("instanceList").getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  private val instanceRetriever = new InstanceRetriever(endpoint, pageSize, graphUri)

  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) =
  {
    instanceRetriever.retrieve(instanceSpec, instanceList union instances)
  }
}