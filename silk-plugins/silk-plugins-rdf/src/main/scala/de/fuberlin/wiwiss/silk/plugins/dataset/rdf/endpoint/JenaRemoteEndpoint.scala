package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}

/**
 * Executes all queries on a remote SPARQL endpoint using Jena.
 */
class JenaRemoteEndpoint(endpointUri: String) extends JenaEndpoint {
  /**
   * Overloaded in subclasses.
   */
  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.sparqlService(endpointUri, query)
  }
}
