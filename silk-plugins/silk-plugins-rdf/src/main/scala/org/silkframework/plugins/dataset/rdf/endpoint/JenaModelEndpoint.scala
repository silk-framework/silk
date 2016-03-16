package org.silkframework.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.{QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.update.{GraphStoreFactory, UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams, SparqlResults}

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
class JenaModelEndpoint(model: Model) extends JenaEndpoint {

  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.create(query, model)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    this.synchronized {
      val graphStore = GraphStoreFactory.create(model)
      UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
    }
  }

  override def select(sparql: String, limit: Int): SparqlResults = {
    this.synchronized {
      super.select(sparql, limit)
    }
  }

  override def construct(query: String): String = {
    this.synchronized {
      super.construct(query)
    }
  }

  override def update(query: String): Unit = {
    this.synchronized {
      super.update(query)
    }
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
    this // SPARQL parameters have no effect
  }
}