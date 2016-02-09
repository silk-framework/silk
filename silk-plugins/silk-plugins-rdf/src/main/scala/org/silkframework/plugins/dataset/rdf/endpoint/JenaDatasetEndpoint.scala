package org.silkframework.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{Dataset, QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.update.{GraphStoreFactory, UpdateExecutionFactory, UpdateFactory, UpdateProcessor}

/**
  * A SPARQL endpoint which executes all queries on a Jena Dataset.
  */
class JenaDatasetEndpoint(dataset: Dataset) extends JenaEndpoint{

  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.create(query, dataset)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    val graphStore = GraphStoreFactory.create(dataset)
    UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
  }

}
