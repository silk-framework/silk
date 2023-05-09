package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.iterator.CloseableIterator

class GenericEntityTable(genericEntities: CloseableIterator[Entity],
                         override val entitySchema: EntitySchema,
                         override val task: Task[TaskSpec],
                         override val globalErrors: Seq[String] = Seq.empty) extends LocalEntities {

  override def entities: CloseableIterator[Entity] = {
    genericEntities
  }

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): GenericEntityTable = {
    new GenericEntityTable(newEntities, newSchema, task)
  }
}

object GenericEntityTable {

  def apply(entities: CloseableIterator[Entity], entitySchema: EntitySchema, task: Task[TaskSpec], globalErrors: Seq[String] = Seq.empty): GenericEntityTable = {
    new GenericEntityTable(entities, entitySchema, task, globalErrors)
  }

  def apply(entities: Iterable[Entity], entitySchema: EntitySchema, task: Task[TaskSpec]): GenericEntityTable = {
    new GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, task)
  }
}