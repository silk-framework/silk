package org.silkframework.entity

import scala.language.implicitConversions

/**
  * A schema for a nested data model.
  */
case class HierarchicalSchema(rootSchemaNode: HierarchicalSchemaNode) extends SchemaTrait

/**
  * A node in the nested schema.
  * @param entitySchema The schema of the entity at this point in the nested schema.
  * @param nestedEntities The child entity schemata of the current entity schema.
  */
case class HierarchicalSchemaNode(entitySchema: EntitySchema, nestedEntities: IndexedSeq[NestedEntitySchema])

/**
  * The connection and node of the child entity schema.
  * @param connection The connection details going from the parent entity to the child entity.
  * @param entitySchemaNode The entity schema node of the child entity.
  */
case class NestedEntitySchema(connection: EntitySchemaConnection, entitySchemaNode: HierarchicalSchemaNode)

/**
  * A connection from the parent entity to its nested entity.
  * @param forwardProperty Optional property to link parent with child entities.
  * @param backwardProperty Optional property to link child with parent entities.
  * @param sourcePath the Silk path from the parent to the child entity in the source data model.
  */
case class EntitySchemaConnection(sourcePath: Path
                                 // The following two would have to be part of the mapping not the source schema
//                                  forwardProperty: Option[Uri],
//                                  backwardProperty: Option[Uri]
                                  )

object HierarchicalSchema {
  implicit def toHierarchicalSchema(entitySchema: EntitySchema): HierarchicalSchema = {
    HierarchicalSchema(HierarchicalSchemaNode(entitySchema, IndexedSeq()))
  }

  implicit def toEntitySchema(hierarchicalSchema: HierarchicalSchema): EntitySchema = {
    assert(hierarchicalSchema.rootSchemaNode.nestedEntities.isEmpty, "Cannot convert nested hierarchical schema to entity schema!")
    val entitySchema = hierarchicalSchema.rootSchemaNode.entitySchema
    entitySchema
  }
}