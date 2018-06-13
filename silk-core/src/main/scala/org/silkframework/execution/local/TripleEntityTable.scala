package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.util.Uri

/**
  * Holds RDF triples.
  */
case class TripleEntityTable(entities: Traversable[Entity], taskOption: Option[Task[TaskSpec]]) extends LocalEntities {

  override def entitySchema: EntitySchema = TripleEntitySchema.schema
}

object TripleEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.TripleSchemaType),
    typedPaths = IndexedSeq(
      TypedPath(Path(SilkVocab.tripleSubject), UriValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.triplePredicate), UriValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.tripleObject), StringValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.tripleObjectValueType), StringValueType, isAttribute = false)
    )
  )
}