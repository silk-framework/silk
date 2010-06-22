package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.Instance

/**
 * DataSource which retrieves all instances from a SPARQL endpoint
 *
 * Parameters:
 * - '''endpointURI''': The URI of the SPARQL endpoint
 * - '''graph (optional)''': Only retrieve instances from a specific graph
 * - '''pageSize (optional)''': The number of solutions to be retrieved per SPARQL query (default: 1000)
 * - '''instanceList (optional)''': The instances to be retrieved. If not given all instances will be retrieved
 * - '''pauseTime (optional)''': The number of milliseconds to wait between subsequent query 
 * - '''retryCount (optional)''': The number of retires if a query fails
 * - '''retryPause (optional)''': The number of milliseconds to wait until a failed query is retried
 */
class SparqlDataSource(val params : Map[String, String]) extends DataSource
{
    private val endpoint = new SparqlEndpoint(
                                   uri = readRequiredParam("endpointURI"),
                                   pauseTime = readOptionalIntParam("pauseTime").getOrElse(0),
                                   retryCount = readOptionalIntParam("retryCount").getOrElse(3),
                                   retryPause = readOptionalIntParam("retryPause").getOrElse(1000)
                               )

    private val graphUri = readOptionalParam("graph")

    private val pageSize = readOptionalIntParam("pageSize").getOrElse(1000)

    private val instanceList = readOptionalParam("instanceList").getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

    private val sparqlExecutor = new SparqlBuilder(endpoint, pageSize, graphUri)

    override def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String] = Map.empty) =
    {
        if(instanceList.isEmpty)
        {
            sparqlExecutor.execute(instanceSpec, None, prefixes)
        }
        else
        {
            for(instance <- instanceList.view) yield sparqlExecutor.execute(instanceSpec, Some(instance), prefixes).head
        }
    }
}
