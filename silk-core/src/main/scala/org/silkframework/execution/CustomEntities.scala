package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}

trait CustomEntities extends EntityHolder {

  def baseEntities: EntityHolder

  override def entitySchema: EntitySchema = baseEntities.entitySchema

  override def entities: Traversable[Entity] = baseEntities.entities

  override def task: Task[TaskSpec] = baseEntities.task

  override def headOption: Option[Entity] = baseEntities.headOption

  override def mapEntities(f: Entity => Entity): EntityHolder = baseEntities.mapEntities(f)

  override def flatMapEntities(outputSchema: EntitySchema, updateTask: Task[TaskSpec])(f: Entity => TraversableOnce[Entity]): EntityHolder = {
    flatMapEntities(outputSchema, updateTask)(f)
  }

  override def filter(f: Entity => Boolean): EntityHolder = baseEntities.filter(f)
}

