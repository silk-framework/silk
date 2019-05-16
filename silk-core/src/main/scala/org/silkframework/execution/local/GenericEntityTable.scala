package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.InterruptibleTraversable

class GenericEntityTable(genericEntities: Traversable[Entity], val entitySchema: EntitySchema, val task: Task[TaskSpec]) extends LocalEntities {
  override def entities: Traversable[Entity] = {
    new InterruptibleTraversable(genericEntities)
  }
}

object GenericEntityTable {
  def apply(entities: Traversable[Entity], entitySchema: EntitySchema, task: Task[TaskSpec]): GenericEntityTable = {
    new GenericEntityTable(entities, entitySchema, task)
  }
}