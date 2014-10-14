package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.rdf.model.Model

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
class JenaModelEndpoint(model: Model) extends JenaEndpoint {
  /**
   * Overloaded in subclasses.
   */
  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.create(query, model)
  }
}
