package org.silkframework.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.update.{UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}

/**
 * Executes all queries on a remote SPARQL endpoint using Jena.
 */
class JenaRemoteEndpoint(endpointUri: String) extends JenaEndpoint {

  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.sparqlService(endpointUri, query)
  }

  override protected def createUpdateExecution(query: String): UpdateProcessor = {
    UpdateExecutionFactory.createRemote(UpdateFactory.create(query), endpointUri)
  }

  /**
    * @return the SPARQL related configuration of this SPARQL endpoint.
    */
  override def sparqlParams: SparqlParams = SparqlParams(pageSize = 0)

  /**
    *
    * @param sparqlParams the new configuration of the SPARQL endpoint.
    * @return A SPARQL endpoint configured with the new parameters.
    */
  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
    this // SPARQL parameters have no effect on this type of endpoint
  }
}
