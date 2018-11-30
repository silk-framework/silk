package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.util.Uri

/** Entity table that holds SPARQL Update queries */
case class SparqlUpdateEntityTable(entities: Traversable[Entity], task: Task[TaskSpec]) extends LocalEntities {

  override def entitySchema: EntitySchema = SparqlUpdateEntitySchema.schema
}

object SparqlUpdateEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.SparqlUpdateSchemaType),
    typedPaths = IndexedSeq(
      TypedPath(Path(SilkVocab.sparqlUpdateQuery), StringValueType, isAttribute = false)
    )
  )
}