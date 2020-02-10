package org.silkframework.dataset.rdf

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.execution.EmptyEntityHolder
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * An entity table that does not contain data, but a SPARQL endpoint from the data source input that can be queried.
  */
class SparqlEndpointEntityTable(sparqlEndpoint: SparqlEndpoint, val task: Task[TaskSpec]) extends LocalEntities with EmptyEntityHolder {

  override def entitySchema: EntitySchema = EntitySchema.empty

  /**
    * Executes the select query on the SPARQL endpoint.
    *
    * @param query   The SELECT query to execute
    * @param limit   The max. number of rows to fetch
    * @param timeout An optional timeout in ms for the query execution. If defined it should have an positive value, else it will be ignored.
    *                This timeout is passed to the underlying SPARQL endpoint implementation.
    */
  def select(query: String,
             limit: Int = Integer.MAX_VALUE,
             timeout: Option[Int] = None)
            (implicit userContext: UserContext): SparqlResults = {
    timeout match {
      case Some(timeoutInMs) if timeoutInMs > 0 =>
        val updatedParams = sparqlEndpoint.sparqlParams.copy(timeout = timeout)
        sparqlEndpoint.withSparqlParams(updatedParams).select(query, limit)
      case _ =>
        sparqlEndpoint.select(query, limit)
    }
  }

  def construct(query: String)(implicit userContext: UserContext): QuadIterator = sparqlEndpoint.construct(query)
}

object SparqlEndpointEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.SparqlEndpointSchemaType),
    typedPaths = IndexedSeq.empty
  )
}