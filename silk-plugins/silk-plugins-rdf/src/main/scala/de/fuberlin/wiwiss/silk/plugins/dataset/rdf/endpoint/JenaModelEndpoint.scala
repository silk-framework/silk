package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.update.{GraphStoreFactory, UpdateExecutionFactory, UpdateFactory, UpdateProcessor}

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
class JenaModelEndpoint(model: Model) extends JenaEndpoint {

  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.create(query, model)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    val graphStore = GraphStoreFactory.create(model)
    UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
  }
}
