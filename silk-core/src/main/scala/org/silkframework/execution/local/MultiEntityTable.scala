package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}

case class MultiEntityTable(entities: Traversable[Entity],
                            entitySchema: EntitySchema,
                            task: Task[TaskSpec],
                            subTables: Seq[LocalEntities]) extends LocalEntities {

  override def updateEntities(newEntities: Traversable[Entity]): LocalEntities = {
    MultiEntityTable(newEntities, entitySchema, task, subTables)
  }
}
