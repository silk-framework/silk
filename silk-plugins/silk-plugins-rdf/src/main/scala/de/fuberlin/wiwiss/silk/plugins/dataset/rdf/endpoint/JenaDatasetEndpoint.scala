package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{Dataset, QueryExecutionFactory, QueryExecution}
import com.hp.hpl.jena.update.{UpdateFactory, UpdateExecutionFactory, GraphStoreFactory, UpdateProcessor}

class JenaDatasetEndpoint(dataset: Dataset) extends JenaEndpoint{

  override protected def createQueryExecution(query: String): QueryExecution = {

    QueryExecutionFactory.create(query, dataset)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    val graphStore = GraphStoreFactory.create(dataset)
    UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
  }

}
