package org.silkframework.execution.local

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.{EntityHolder, EntityHolderWithEntityIterator}
import org.silkframework.util.Uri

/**
  * A local table of entities.
  */
trait LocalEntities extends EntityHolder {

  /**
    * get head Entity
    */
  override def headOption: Option[Entity] = this.entities.headOption
}

trait LocalEntitiesWithIterator extends LocalEntities with EntityHolderWithEntityIterator


/** This should be used if no input is explicitly "requested". E.g. when the subsequent task signals to a data source
  * that it needs no input data, the data source should send an instance of [[EmptyEntityTable]]. */
case class EmptyEntityTable(task: Task[TaskSpec]) extends LocalEntities {
  override def entitySchema: EntitySchema = EmptyEntityTable.schema

  override def entities: Traversable[Entity] = Seq.empty[Entity]
}

object EmptyEntityTable {
  /** This schema should be requested to explicitly signal the previous task to not generate any entities at all. */
  final val schema = EntitySchema(
    typeUri = Uri(SilkVocab.EmptySchemaType),
    typedPaths = IndexedSeq()
  )
}