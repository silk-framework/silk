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
abstract class TypedEntitySchema[EntityType] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema.
   */
  def schema: EntitySchema

  /**
   * Creates a generic entity from a custom value.
   */
  def toEntity(v: EntityType)(implicit pluginContext: PluginContext): Entity

  /**
   * Creates a custom value from a generic entity.
   */
  def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): EntityType

  /**
   * Converts a generic entity table to typed entities.
   * Enables implementation classes to be used in pattern matching.
   */
  def unapply(entities: LocalEntities)(implicit pluginContext: PluginContext): Option[TypedEntities[EntityType]] = {
    entities match {
      //TODO type erasure?
      case customEntities: TypedEntities[EntityType] =>
        Some(customEntities)
      case _ if entities.entitySchema.typeUri == schema.typeUri =>
        Some(new TypedEntities[EntityType](
          typedEntities = entities.entities.map(fromEntity),
          typedEntitySchema = this,
          task = entities.task
        ))
      case _ =>
        None
    }
  }

  /**
   * Creates new local typed entities.
   */
  def create(values: CloseableIterator[EntityType], task: Task[TaskSpec])
            (implicit pluginContext: PluginContext): TypedEntities[EntityType] = {
    new TypedEntities(values, this, task)
  }
}
