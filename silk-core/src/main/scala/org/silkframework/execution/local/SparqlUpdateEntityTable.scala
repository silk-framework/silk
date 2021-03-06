package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.{EntityHolder, InterruptibleTraversable}
import org.silkframework.util.Uri

/** Entity table that holds SPARQL Update queries */
class SparqlUpdateEntityTable(entityTraversable: Traversable[Entity], val task: Task[TaskSpec]) extends LocalEntities {

  override def entitySchema: EntitySchema = SparqlUpdateEntitySchema.schema

  override def entities: Traversable[Entity] = {
    new InterruptibleTraversable[Entity](entityTraversable)
  }

  override def updateEntities(newEntities: Traversable[Entity], newSchema: EntitySchema): LocalEntities = {
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