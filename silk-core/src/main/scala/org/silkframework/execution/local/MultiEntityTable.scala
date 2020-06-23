package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}

case class MultiEntityTable(entities: Traversable[Entity],
                            entitySchema: EntitySchema,
                            task: Task[TaskSpec],
                            subTables: Seq[LocalEntities],
                            override val globalErrors: Seq[String] = Seq.empty) extends LocalEntities {

  override def updateEntities(newEntities: Traversable[Entity]): LocalEntities = {
    MultiEntityTable(newEntities, entitySchema, task, subTables)
  }
}
