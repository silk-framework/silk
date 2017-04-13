package org.silkframework.execution.local

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.{NestedEntity, NestedEntitySchema}

/**
  * An table consisting of nested entities
  */
case class NestedEntityTable(entities: Traversable[NestedEntity],
                             entitySchema: NestedEntitySchema,
                             task: Task[TaskSpec]) extends NestedEntityTableTrait
