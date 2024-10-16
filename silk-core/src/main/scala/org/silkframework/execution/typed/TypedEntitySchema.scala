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
   * User-readable name for this schema.
   */
  def name: String = getClass.getName

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema, i.e., the type URI must be unique.
   */
  def schema: EntitySchema

  /**
   * Creates a generic entity from a typed entity.
   */
  def toEntity(entity: EntityType)(implicit pluginContext: PluginContext): Entity

  /**
   * Creates a typed entity from a generic entity.
   * Implementations may assume that the incoming schema matches the schema expected by this typed schema, i.e., schema validation is not required.
   */
  def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): EntityType

  /**
   * Makes sure that a provided schema matches this schema.
   *
   * @throws WrongEntitySchemaException If the schema does not match.
   */
  def validateSchema(entitySchema: EntitySchema): Unit = {
    if(entitySchema.typeUri != schema.typeUri) {
      throw WrongEntitySchemaException(s"Expected entities of schema '$name' with type ${schema.typeUri}. Received entities of type ${entitySchema.typeUri}.")
    }
    if(entitySchema.typedPaths.size != schema.typedPaths.size) {
      throw WrongEntitySchemaException(s"Schema '$name' expects ${schema.typedPaths.size} paths. Received ${entitySchema.typedPaths.size} paths.")
    }
    for(((receivedPath, expectedPath), index) <- (entitySchema.typedPaths zip schema.typedPaths).zipWithIndex) {
      // We only check the path itself, but allow different value types
      if(receivedPath.normalizedSerialization != expectedPath.normalizedSerialization) {
        throw WrongEntitySchemaException(
          s"Schema '$name' expects the path ${expectedPath.normalizedSerialization} at index $index. Received ${receivedPath.normalizedSerialization}.")
      }
    }
  }

  /**
   * Converts a generic entity table to typed entities.
   * Enables implementation classes to be used in pattern matching.
   *
   * @throws WrongEntitySchemaException If the schema does have the expected type URI, but the paths do not match the expected paths.
   * @return The parsed typed entities, if the type URI matches. None, otherwise.
   */
  def unapply(entities: LocalEntities)(implicit pluginContext: PluginContext): Option[TypedEntities[EntityType, TaskType]] = {
    if(entities.entitySchema.typeUri == schema.typeUri) {
      validateSchema(entities.entitySchema)
      // Since we assume that the type URI is unique, we can safely cast to the expected types.
      if(entities.isInstanceOf[TypedEntities[_, _]]) {
        Some(entities.asInstanceOf[TypedEntities[EntityType, TaskType]])
      } else {
        Some(new TypedEntities[EntityType, TaskType](
          typedEntities = entities.entities.map(fromEntity),
          typedEntitySchema = this,
          task = entities.task.asInstanceOf[Task[TaskType]]
        ))
      }
    } else {
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

case class WrongEntitySchemaException(msg: String) extends RuntimeException(msg)
