package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.{EmptyEntityHolder, EntityHolder}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.Uri

/**
  * A local table of entities.
  */
trait LocalEntities extends EntityHolder {

  /**
    * get head Entity
    */
  override def headOption: Option[Entity] = entities.headOption

  def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities

  def mapEntities(f: Entity => Entity): EntityHolder = {
    updateEntities(entities.map(f), entitySchema)
  }

  def flatMapEntities(outputSchema: EntitySchema, updateTask: Task[TaskSpec] = task)(f: Entity => Iterator[Entity]): EntityHolder = {
    updateEntities(entities.flatMap(f), outputSchema)
  }

  def filter(f: Entity => Boolean): EntityHolder = {
    updateEntities(CloseableIterator(entities.filter(f), entities), entitySchema)
  }

  /**
    * Closes the entities iterator.
    */
  override final def close(): Unit = {
    entities.close()
  }

}

/** This should be used if no input is explicitly "requested". E.g. when the subsequent task signals to a data source
  * that it needs no input data, the data source should send an instance of [[EmptyEntityTable]]. */
case class EmptyEntityTable(task: Task[TaskSpec]) extends LocalEntities with EmptyEntityHolder {
  override def entitySchema: EntitySchema = EmptyEntityTable.schema
}

object EmptyEntityTable {
  /** This schema should be requested to explicitly signal the previous task to not generate any entities at all. */
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.EmptySchemaType),
    typedPaths = IndexedSeq()
  )
}