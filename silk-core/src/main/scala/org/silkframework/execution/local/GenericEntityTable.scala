package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}

class GenericEntityTable(genericEntities: Traversable[Entity],
                         override val entitySchema: EntitySchema,
                         override val task: Task[TaskSpec],
                         override val globalErrors: Seq[String] = Seq.empty) extends LocalEntities {

  override def entities: Traversable[Entity] = {
    genericEntities
  }

  override def updateEntities(newEntities: Traversable[Entity], newSchema: EntitySchema): GenericEntityTable = {
    new GenericEntityTable(newEntities, newSchema, task)
  }
}

object GenericEntityTable {
  def apply(entities: Traversable[Entity], entitySchema: EntitySchema, task: Task[TaskSpec], globalErrors: Seq[String] = Seq.empty): GenericEntityTable = {
    new GenericEntityTable(entities, entitySchema, task, globalErrors)
  }
}