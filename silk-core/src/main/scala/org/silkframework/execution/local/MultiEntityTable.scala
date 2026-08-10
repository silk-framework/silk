package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.iterator.CloseableIterator

import scala.util.control.NonFatal

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

  /**
    * Closes this table and all nested tables.
    * Sub tables that a consumer did not read must be closed as well, since they may hold resources and
    * report their completion on close.
    * All closes are attempted even if one of them fails; the first failure is rethrown.
    */
  override def close(): Unit = {
    var error: Option[Throwable] = None
    def attempt(close: => Unit): Unit = {
      try {
        close
      } catch {
        case NonFatal(ex) =>
          error match {
            case Some(first) => first.addSuppressed(ex)
            case None => error = Some(ex)
          }
      }
    }
    attempt(super.close())
    for(table <- subTables) {
      attempt(table.close())
    }
    error.foreach(ex => throw ex)
  }
}
