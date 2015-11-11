package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.update.{UpdateFactory, UpdateExecutionFactory, UpdateProcessor}

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
}
