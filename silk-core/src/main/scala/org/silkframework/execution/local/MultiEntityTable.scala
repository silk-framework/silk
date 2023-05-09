package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.iterator.CloseableIterator

case class MultiEntityTable(entities: CloseableIterator[Entity],
                            entitySchema: EntitySchema,
                            task: Task[TaskSpec],
                            subTables: Seq[LocalEntities],
                            override val globalErrors: Seq[String] = Seq.empty) extends LocalEntities {

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = {
    MultiEntityTable(newEntities, newSchema, task, subTables)
  }
}
