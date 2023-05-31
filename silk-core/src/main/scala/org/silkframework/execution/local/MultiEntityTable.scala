package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.iterator.CloseableIterator

case class MultiEntityTable(entities: CloseableIterator[Entity],
                            entitySchema: EntitySchema,
                            task: Task[TaskSpec],
                            subTables: Seq[LocalEntities],
                            override val globalErrors: Seq[String] = Seq.empty) extends LocalEntities {

  /**
    * Collects this entity table and all (recursively) nested entity tables.
    */
  def allTables: Seq[LocalEntities] = {
    this +: subTables.flatMap {
      case mt: MultiEntityTable =>
        mt.allTables
      case et: LocalEntities =>
        Seq(et)
    }
  }

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = {
    MultiEntityTable(newEntities, newSchema, task, subTables)
  }
}
