package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.InterruptibleIterator
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.Uri

/** Entity table that holds SPARQL Update queries */
class SparqlUpdateEntityTable(entityTraversable: CloseableIterator[Entity], val task: Task[TaskSpec]) extends LocalEntities {

  override def entitySchema: EntitySchema = SparqlUpdateEntitySchema.schema

  override def entities: CloseableIterator[Entity] = {
    new InterruptibleIterator[Entity](entityTraversable)
  }

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = {
    new SparqlUpdateEntityTable(newEntities, task)
  }
}

object SparqlUpdateEntitySchema {
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.SparqlUpdateSchemaType),
    typedPaths = IndexedSeq(
      TypedPath(UntypedPath(SilkVocab.sparqlUpdateQuery), ValueType.STRING, isAttribute = false)
    )
  )
}