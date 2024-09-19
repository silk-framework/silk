package org.silkframework.execution.typed

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext

/**
 * Collection of entities of a particular type.
 *
 * @param typedEntities The typed entities.
 * @param typedEntitySchema The schema that defines how the typed entities are identified and translated from/to generic entity tables.
 * @param task The task that generated these entities.
 * @param pluginContext The plugin context that will be used to (de-)serialize typed entities to generic entity tables.
 * @tparam EntityType The type of the entities in this collection.
 */
class TypedEntities[EntityType](val typedEntities: CloseableIterator[EntityType],
                                val typedEntitySchema: TypedEntitySchema[EntityType],
                                override val task: Task[TaskSpec])
                               (implicit pluginContext: PluginContext) extends LocalEntities {

  /**
   * The schema of the entities
   */
  override def entitySchema: EntitySchema = typedEntitySchema.schema

  /**
   * The generic entities.
   */
  override def entities: CloseableIterator[Entity] = {
    typedEntities.map(typedEntitySchema.toEntity)
  }

  /**
   * Returns a copy of this collection that contains user-provided entities.
   *
   * @param newEntities The updated entities.
   * @param newSchema The updated schema.
   * @return If the schema is unchanged -> returns a copy of this class.
   *         If the schema is changed -> returns a generic entity table.
   */
  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = {
    if(newSchema == entitySchema) {
      new TypedEntities[EntityType](
        typedEntities = newEntities.map(typedEntitySchema.fromEntity),
        typedEntitySchema = typedEntitySchema,
        task = task
      )
    } else {
      new GenericEntityTable(newEntities, newSchema, task)
    }
  }
}