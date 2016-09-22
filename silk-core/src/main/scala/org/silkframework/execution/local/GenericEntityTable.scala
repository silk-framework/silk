package org.silkframework.execution.local

import org.silkframework.entity.{Entity, EntitySchema}

case class GenericEntityTable(entities: Traversable[Entity], entitySchema: EntitySchema) extends EntityTable
