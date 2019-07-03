package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.InterruptibleTraversable
import org.silkframework.util.Uri

case class QuadEntityTable(entityFunction: () => Traversable[Entity], task: Task[TaskSpec]) extends LocalEntities {

  override def entitySchema: EntitySchema = QuadEntityTable.schema

  override def entities: Traversable[Entity] = new InterruptibleTraversable(entityFunction())
}

object QuadEntityTable {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.QuadSchemaType),
    typedPaths = IndexedSeq(
      TypedPath(UntypedPath(SilkVocab.tripleSubject), UriValueType, isAttribute = false),
      TypedPath(UntypedPath(SilkVocab.triplePredicate), UriValueType, isAttribute = false),
      TypedPath(UntypedPath(SilkVocab.tripleObject), StringValueType, isAttribute = false),
      TypedPath(UntypedPath(SilkVocab.tripleObjectValueType), StringValueType, isAttribute = false),
      TypedPath(UntypedPath(SilkVocab.quadContext), StringValueType, isAttribute = false)
    )
  )
}
