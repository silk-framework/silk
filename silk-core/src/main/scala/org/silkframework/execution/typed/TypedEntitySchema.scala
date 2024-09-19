package org.silkframework.execution.typed

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext

/**
 * A custom entity schema that holds entities of a specific type (e.g. files).
 *
 * @tparam EntityType The type of entities to be held.
 */
abstract class TypedEntitySchema[EntityType, TaskType <: TaskSpec] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema.
   */
  def schema: EntitySchema

  /**
   * Creates a generic entity from a typed entity.
   */
  def toEntity(entity: EntityType)(implicit pluginContext: PluginContext): Entity

  /**
   * Creates a typed entity from a generic entity.
   */
  def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): EntityType

  /**
   * Converts a generic entity table to typed entities.
   * Enables implementation classes to be used in pattern matching.
   */
  def unapply(entities: LocalEntities)(implicit pluginContext: PluginContext): Option[TypedEntities[EntityType, TaskType]] = {
    entities match {
      //TODO type erasure?
      case customEntities: TypedEntities[EntityType, TaskType] =>
        Some(customEntities)
      case _ if entities.entitySchema.typeUri == schema.typeUri =>
        Some(new TypedEntities[EntityType, TaskType](
          typedEntities = entities.entities.map(fromEntity),
          typedEntitySchema = this,
          //TODO check for wrong type and throw error
          task = entities.task.asInstanceOf[Task[TaskType]]
        ))
      case _ =>
        None
    }
  }

  /**
   * Checks if a given entity table can be converted to this type.
   */
  def hasType(entities: LocalEntities)(implicit pluginContext: PluginContext): Boolean = {
    unapply(entities).isDefined
  }

  /**
   * Creates new local typed entities.
   */
  def create(values: CloseableIterator[EntityType], task: Task[TaskType])
            (implicit pluginContext: PluginContext): TypedEntities[EntityType, TaskType] = {
    new TypedEntities(values, this, task)
  }

  /**
   * Creates new local typed entities.
   */
  def create(values: Iterable[EntityType], task: Task[TaskType])
            (implicit pluginContext: PluginContext): TypedEntities[EntityType, TaskType] = {
    new TypedEntities(CloseableIterator(values), this, task)
  }

  /**
   * Creates new empty typed entities.
   */
  def create(task: Task[TaskType])
            (implicit pluginContext: PluginContext): TypedEntities[EntityType, TaskType] = {
    new TypedEntities(CloseableIterator.empty, this, task)
  }

}
