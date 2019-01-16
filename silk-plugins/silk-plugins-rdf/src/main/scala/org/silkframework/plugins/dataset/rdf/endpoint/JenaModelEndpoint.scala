package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{Query, QueryExecution, QueryExecutionFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.sparql.core.DatasetGraphFactory
import org.apache.jena.update.{UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.silkframework.dataset.rdf.{QuadIterator, SparqlEndpoint, SparqlParams, SparqlResults}
import org.silkframework.runtime.activity.UserContext

/**
 * A SPARQL endpoint which executes all queries on a Jena Model.
 */
class JenaModelEndpoint(model: Model) extends JenaEndpoint {

  override protected def createQueryExecution(query: Query): QueryExecution = {
    QueryExecutionFactory.create(query, model)
  }

  override def addGraph(query: Query): Unit = {
    // Do not add graph since there are no graphs
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    this.synchronized {
      val graphStore = DatasetGraphFactory.wrap(model.getGraph)
      UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
    }
  }

  override def select(sparql: String, limit: Int)
                     (implicit userContext: UserContext): SparqlResults = {
    this.synchronized {
      super.select(sparql, limit)
    }
  }

  override def construct(query: String)
                        (implicit userContext: UserContext): QuadIterator= {
    this.synchronized {
      super.construct(query)
    }
  }

  override def update(query: String)
                     (implicit userContext: UserContext): Unit = {
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