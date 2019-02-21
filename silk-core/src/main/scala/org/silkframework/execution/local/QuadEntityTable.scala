package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.util.Uri

case class QuadEntityTable(entityFunction: () => Traversable[Entity], task: Task[TaskSpec]) extends LocalEntities {

  override def entitySchema: EntitySchema = QuadEntityTable.schema

  override def entities: Traversable[Entity] = entityFunction()
}

object QuadEntityTable {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.QuadSchemaType),
    typedPaths = IndexedSeq(
      TypedPath(Path(SilkVocab.tripleSubject), UriValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.triplePredicate), UriValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.tripleObject), StringValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.tripleObjectValueType), StringValueType, isAttribute = false),
      TypedPath(Path(SilkVocab.quadContext), StringValueType, isAttribute = false)
    )
  )
}
