package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}

case class MultiEntityTable(entities: Traversable[Entity], entitySchema: EntitySchema, task: Task[TaskSpec], subTables: Seq[SubTable]) extends EntityTable

case class SubTable(entities: Traversable[Entity], entitySchema: EntitySchema, task: Task[TaskSpec]) extends EntityTable
