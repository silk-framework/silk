package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.util.Uri

/**
  * Holds RDF triples.
  */
case class TripleEntityTable(entities: Traversable[Entity], task: Task[TaskSpec]) extends EntityTable {
  override def entitySchema: EntitySchema = TripleEntitySchema.schema
}

object TripleEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.TripleSchemaType),
    paths = IndexedSeq(
      Path(SilkVocab.tripleSubject),
      Path(SilkVocab.triplePredicate),
      Path(SilkVocab.tripleObject)
    )
  )
}
