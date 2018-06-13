package org.silkframework.dataset.rdf

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.execution.local.LocalEntities
import org.silkframework.util.Uri

/**
  * An entity table that does not contain data, but a SPARQL endpoint from the data source input that can be queried.
  */
class SparqlEndpointEntityTable(sparqlEndpoint: SparqlEndpoint, val taskOption: Option[Task[TaskSpec]]) extends LocalEntities {
  override def entitySchema: EntitySchema = EntitySchema.empty

  override def entities: Traversable[Entity] = Traversable.empty

  def select(query: String, limit: Int = Integer.MAX_VALUE): SparqlResults = sparqlEndpoint.select(query, limit)
}

object SparqlEndpointEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.SparqlEndpointSchemaType),
    typedPaths = IndexedSeq.empty
  )
}