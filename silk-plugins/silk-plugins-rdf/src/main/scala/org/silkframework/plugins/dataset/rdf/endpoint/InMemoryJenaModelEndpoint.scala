package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{DatasetFactory, Query, QueryExecution, QueryExecutionFactory}
import org.apache.jena.rdf.listeners.StatementListener
import org.apache.jena.rdf.model.{Model, ModelFactory, Statement}
import org.apache.jena.update.{UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.silkframework.dataset.rdf.{QuadIterator, SparqlEndpoint, SparqlParams, SparqlResults}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource

/**
 * A SPARQL endpoint that owns its Jena [[Model]] internally.
 *
 * Enforeces an in-memory size limit based on [[Resource.maxInMemorySize]].
 */
class InMemoryJenaModelEndpoint extends JenaEndpoint {

  private val model: Model = ModelFactory.createDefaultModel()

  private val byteLimit: Long = Resource.maxInMemorySize()
  @volatile private var estimatedBytesWritten: Long = 0L

  model.register(new StatementListener {
    override def addedStatement(s: Statement): Unit =
      estimatedBytesWritten += statementBytes(s)
    override def removedStatement(s: Statement): Unit =
      estimatedBytesWritten = math.max(0L, estimatedBytesWritten - statementBytes(s))
  })

  private def statementBytes(s: Statement): Long =
    s.getSubject.toString.length.toLong +
    s.getPredicate.toString.length.toLong +
    s.getObject.toString.length.toLong

  override protected def createQueryExecution(query: Query): QueryExecution = {
    QueryExecutionFactory.create(query, model)
  }

  override def addGraph(query: Query): Unit = {
    // Do not add graph since there are no graphs
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    this.synchronized {
      val graphStore = DatasetFactory.wrap(model)
      UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
    }
  }

  override def select(sparql: String, limit: Int = Integer.MAX_VALUE)
                     (implicit userContext: UserContext): SparqlResults = {
    this.synchronized {
      super.select(sparql, limit)
    }
  }

  override def construct(query: String)
                        (implicit userContext: UserContext): QuadIterator = {
    this.synchronized {
      super.construct(query)
    }
  }

  override def update(query: String)
                     (implicit userContext: UserContext): Unit = {
    this.synchronized {
      super.update(query)
      if (estimatedBytesWritten > byteLimit) {
        throw new RuntimeException(
          s"In-memory Knowledge Graph has exceeded the size limit of $byteLimit bytes " +
          s"(estimated bytes written: $estimatedBytesWritten). " +
          s"Reduce the amount of data written or increase the limit by configuring " +
          s"'${Resource.maxInMemorySizeParameterName}'."
        )
      }
    }
  }

  override def sparqlParams: SparqlParams = SparqlParams(pageSize = 0)

  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
    this // SPARQL parameters have no effect
  }
}
